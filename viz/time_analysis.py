#!/usr/bin/env python3
"""Run CIM timing analyses varying N or M and plot mean time with error bars.

Examples:
    python viz/time_analysis.py --variable m --values 3 4 5 6 7 8 9 10 --runs-per-value 10 --n 100
    python viz/time_analysis.py --variable n --values 100 200 300 400 --runs-per-value 10 --m 8
    python viz/time_analysis.py --variable n --values 10 50 100 200 300 400 --compare-density --density-reference-n 200
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import shutil
import statistics
import subprocess
import sys
from datetime import datetime
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


def positive_int(raw: str) -> int:
    value = int(raw)
    if value <= 0:
        raise argparse.ArgumentTypeError("debe ser mayor a 0")
    return value


def non_negative_int(raw: str) -> int:
    value = int(raw)
    if value < 0:
        raise argparse.ArgumentTypeError("debe ser mayor o igual a 0")
    return value


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Corre multiples ejecuciones del CIM variando N o M y genera un grafico de tiempos."
    )
    parser.add_argument("--variable", choices=("n", "m"), help="Variable a barrer.")
    parser.add_argument("--values", nargs="+", type=positive_int, help="Valores de N o M a probar.")
    parser.add_argument("--runs-per-value", type=positive_int, default=10, help="Corridas por cada valor.")
    parser.add_argument("--seed", type=int, default=12345, help="Seed usada para generar sistemas reproducibles.")
    parser.add_argument("--n", type=positive_int, default=100, help="N fijo cuando --variable=m.")
    parser.add_argument("--m", type=positive_int, default=10, help="M fijo cuando --variable=n.")
    parser.add_argument("--l", type=float, default=20.0, help="Lado del area.")
    parser.add_argument("--rc", type=float, default=1.0, help="Radio de interaccion.")
    parser.add_argument("--radius-min", type=float, default=0.23, help="Radio minimo de particula.")
    parser.add_argument("--radius-max", type=float, default=0.26, help="Radio maximo de particula.")
    parser.add_argument("--periodic", action="store_true", help="Usar condiciones periodicas.")
    parser.add_argument("--target", type=positive_int, default=1, help="Particula objetivo si se habilita visualizacion.")
    parser.add_argument("--java", default="java", help="Ejecutable de Java.")
    parser.add_argument(
        "--classpath",
        default=f"src/main/resources{';' if sys.platform.startswith('win') else ':'}target/classes",
        help="Classpath para ejecutar ar.edu.itba.sds.Main.",
    )
    parser.add_argument(
        "--compile",
        action="store_true",
        help="Compila src/main/java con javac antes de correr el analisis.",
    )
    parser.add_argument("--javac", default="javac", help="Ejecutable de javac si se usa --compile.")
    parser.add_argument("--release", type=non_negative_int, default=21, help="Release de Java para javac.")
    parser.add_argument("--show", action="store_true", help="Abre una ventana con el grafico al terminar.")
    parser.add_argument(
        "--compare-density",
        action="store_true",
        help=(
            "Punto 4.2: con --variable=n, superpone la curva de L fijo (densidad libre) "
            "y la de densidad fija obtenida aumentando L junto con N."
        ),
    )
    parser.add_argument(
        "--density-reference-n",
        type=positive_int,
        help=(
            "N intermedio cuya densidad N/L^2 se mantiene en la curva de densidad fija. "
            "Debe ser uno de --values; si se omite se usa el valor central."
        ),
    )
    parser.add_argument(
        "--replot-dir",
        type=Path,
        help="Regenera el PNG desde una carpeta time_<tipo>_<timestamp> ya guardada, sin ejecutar Java.",
    )
    return parser.parse_args()


def compile_sources(args: argparse.Namespace) -> None:
    java_files = sorted(Path("src/main/java").rglob("*.java"))
    if not java_files:
        raise RuntimeError("No se encontraron archivos Java en src/main/java")

    Path("target/classes").mkdir(parents=True, exist_ok=True)
    command = [
        args.javac,
        "--release",
        str(args.release),
        "-encoding",
        "UTF-8",
        "-d",
        "target/classes",
        *[str(path) for path in java_files],
    ]
    subprocess.run(command, check=True)


def java_base_args(args: argparse.Namespace) -> list[str]:
    return [
        f"--rc={args.rc}",
        f"--radius-min={args.radius_min}",
        f"--radius-max={args.radius_max}",
        f"--periodic={str(args.periodic).lower()}",
        f"--target={args.target}",
        "--viz-enabled=false",
    ]


def run_main(args: argparse.Namespace, cli_args: list[str], time_file: Path) -> int:
    command = [args.java, "-cp", args.classpath, "ar.edu.itba.sds.Main", *cli_args]
    completed = subprocess.run(command, text=True, capture_output=True)
    if completed.returncode != 0:
        raise RuntimeError(
            "Fallo una corrida de Java.\n"
            f"Comando: {' '.join(command)}\n"
            f"stdout:\n{completed.stdout}\n"
            f"stderr:\n{completed.stderr}"
        )

    if not time_file.exists():
        raise RuntimeError(f"Main no escribio el archivo de tiempo esperado: {time_file}")
    return int(time_file.read_text(encoding="utf-8").strip())


def copy_if_exists(source: Path, destination: Path) -> None:
    if source.exists():
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)


def delete_if_exists(path: Path) -> None:
    if path.exists():
        path.unlink()


def paths_for_value(run_root: Path, variable: str, value: int) -> dict[str, Path]:
    value_dir = run_root / f"{variable}_{value}"
    input_dir = value_dir / "input"
    output_dir = value_dir / "outputs"
    return {
        "static": input_dir / "static.txt",
        "dynamic": input_dir / "dynamic.txt",
        "outputs": output_dir,
        "neighbours": value_dir / "latest_neighbours.txt",
        "time": value_dir / "latest_time.txt",
    }


def run_analysis(
    args: argparse.Namespace,
    run_root: Path,
    *,
    series: str = "standard",
    density_reference_n: int | None = None,
) -> list[dict[str, float | int | str]]:
    rows: list[dict[str, float | int | str]] = []

    shared_m_input_paths = paths_for_value(run_root, "m_fixed_particles", args.n)
    base = java_base_args(args)

    for value in args.values:
        if series == "fixed_density" and value < density_reference_n:
            continue

        path_variable = args.variable if series == "standard" else f"{args.variable}_{series}"
        value_paths = paths_for_value(run_root, path_variable, value)
        if args.variable == "m":
            value_paths["static"] = shared_m_input_paths["static"]
            value_paths["dynamic"] = shared_m_input_paths["dynamic"]
        value_paths["outputs"].mkdir(parents=True, exist_ok=True)

        for run_number in range(1, args.runs_per_value + 1):
            n_value = args.n if args.variable == "m" else value
            m_value = value if args.variable == "m" else args.m
            # Usa la misma precision que StaticFileWriter para L.
            base_l = round(args.l, 4)
            l_value = base_l
            if series == "fixed_density":
                l_value = round(base_l * math.sqrt(n_value / density_reference_n), 4)
                # Mantiene aproximadamente el largo de celda optimo L/M hallado
                # en 4.1. floor evita crear celdas mas pequenas que las originales.
                m_value = max(1, math.floor(args.m * l_value / base_l + 1e-12))
            # Cada medicion usa un sistema nuevo. Las semillas consecutivas
            # hacen distintas las corridas, pero la corrida k usa la misma
            # semilla para todos los valores de M y permite compararlos.
            value_paths["static"].unlink(missing_ok=True)
            value_paths["dynamic"].unlink(missing_ok=True)
            run_seed = args.seed + run_number - 1

            cli_args = [
                *base,
                f"--n={n_value}",
                f"--m={m_value}",
                f"--l={l_value:.12g}",
                "--input-mode=random",
                f"--random-seed={run_seed}",
                f"--static-file={value_paths['static']}",
                f"--dynamic-file={value_paths['dynamic']}",
                f"--neighbours-file={value_paths['neighbours']}",
                f"--time-file={value_paths['time']}",
            ]
            elapsed_ns = run_main(args, cli_args, value_paths["time"])

            run_prefix = value_paths["outputs"] / f"run_{run_number:03d}"
            copy_if_exists(value_paths["neighbours"], run_prefix.with_suffix(".neighbours.txt"))
            copy_if_exists(value_paths["time"], run_prefix.with_suffix(".time.txt"))

            rows.append(
                {
                    "variable": args.variable.upper(),
                    "value": value,
                    "run": run_number,
                    "n": n_value,
                    "m": m_value,
                    "l": l_value,
                    "density": n_value / (l_value * l_value),
                    "series": series,
                    "elapsed_ns": elapsed_ns,
                    "elapsed_ms": elapsed_ns / 1_000_000,
                }
            )
            print(
                f"{series}: {args.variable.upper()}={value}, L={l_value:.6g}, M={m_value}, "
                f"corrida {run_number}/{args.runs_per_value}, seed={run_seed}: {elapsed_ns} ns"
            )

    return rows


def aggregate(rows: list[dict[str, float | int | str]]) -> list[dict[str, float | int]]:
    grouped: dict[int, list[float]] = {}
    for row in rows:
        grouped.setdefault(int(row["value"]), []).append(float(row["elapsed_ms"]))

    summary = []
    for value in sorted(grouped):
        samples = grouped[value]
        mean = statistics.fmean(samples)
        stdev = statistics.stdev(samples) if len(samples) > 1 else 0.0
        stderr = stdev / math.sqrt(len(samples)) if samples else 0.0
        summary.append(
            {
                "value": value,
                "runs": len(samples),
                "mean_ms": mean,
                "stdev_ms": stdev,
                "stderr_ms": stderr,
            }
        )
    return summary


def aggregate_by_series(rows: list[dict[str, float | int | str]]) -> dict[str, list[dict[str, float | int]]]:
    grouped: dict[str, list[dict[str, float | int | str]]] = {}
    for row in rows:
        grouped.setdefault(str(row["series"]), []).append(row)
    return {series: aggregate(series_rows) for series, series_rows in grouped.items()}


def plot_summary(path: Path, args: argparse.Namespace, summary: list[dict[str, float | int]]) -> None:
    x_values = [int(row["value"]) for row in summary]
    means = [float(row["mean_ms"]) for row in summary]
    errors = [float(row["stdev_ms"]) for row in summary]

    path.parent.mkdir(parents=True, exist_ok=True)
    fig, ax = plt.subplots(figsize=(7.2, 4.6), dpi=160)
    ax.errorbar(
        x_values,
        means,
        yerr=errors,
        color="#4f86c6",
        ecolor="#6f6f6f",
        marker="o",
        markersize=4.8,
        linewidth=1.8,
        elinewidth=1.1,
        capsize=3.5,
    )
    variable = args.variable.upper()
    fixed_label = f"N={args.n:g}" if args.variable == "m" else f"M={args.m:g}"
    ax.set_xlabel(variable)
    ax.set_ylabel("Tiempo CIM promedio (ms)")
    boundary_label = "periodico" if args.periodic else "no periodico"
    ax.set_title(
        f"Tiempo de ejecucion variando {variable} | L={args.l:g}, rc={args.rc:g}, {fixed_label}, {boundary_label}",
        loc="left",
        fontsize=11,
    )
    ax.grid(axis="y", color="#dddddd", linewidth=0.8)
    ax.grid(axis="x", visible=False)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.spines["left"].set_color("#c8c8c8")
    ax.spines["bottom"].set_color("#c8c8c8")
    ax.tick_params(colors="#666666")
    ax.set_xticks(x_values)
    fig.tight_layout()
    fig.savefig(path)
    plt.close(fig)


def plot_density_comparison(
    path: Path,
    args: argparse.Namespace,
    free_summary: list[dict[str, float | int]],
    fixed_summary: list[dict[str, float | int]],
    reference_n: int,
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    density = reference_n / (args.l * args.l)
    fig, ax = plt.subplots(figsize=(7.8, 5.0), dpi=160)

    series = (
        (free_summary, "#2a78d6", "o", f"Densidad libre (L={args.l:g})"),
        (
            fixed_summary,
            "#eb6834",
            "s",
            f"Densidad fija (rho={density:.4g}, L y M variables)",
        ),
    )
    for summary, color, marker, label in series:
        ax.errorbar(
            [int(row["value"]) for row in summary],
            [float(row["mean_ms"]) for row in summary],
            yerr=[float(row["stdev_ms"]) for row in summary],
            color=color,
            ecolor=color,
            marker=marker,
            label=label,
            markersize=5.2,
            linewidth=1.8,
            elinewidth=1.1,
            capsize=3.5,
        )

    boundary_label = "periodico" if args.periodic else "no periodico"
    ax.set_xlabel("N")
    ax.set_ylabel("Tiempo CIM promedio (ms)")
    ax.set_title(
        f"Tiempo variando N: densidad libre vs. fija | rc={args.rc:g}, {boundary_label}\n"
        f"Referencia de densidad: N={reference_n}, L={args.l:g}; M base={args.m}",
        loc="left",
        fontsize=11,
    )
    ax.grid(axis="y", color="#dddddd", linewidth=0.8)
    ax.grid(axis="x", visible=False)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.spines["left"].set_color("#c8c8c8")
    ax.spines["bottom"].set_color("#c8c8c8")
    ax.tick_params(colors="#666666")
    ax.set_xticks(sorted({int(row["value"]) for row in free_summary}))
    ax.legend(frameon=False)
    fig.tight_layout()
    fig.savefig(path)
    plt.close(fig)


def write_csv(path: Path, rows: list[dict], fieldnames: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as output:
        writer = csv.DictWriter(output, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", newline="", encoding="utf-8") as input_file:
        return list(csv.DictReader(input_file))


def write_metadata(path: Path, args: argparse.Namespace, timestamp: str, suffix: str, reference_n: int | None) -> None:
    metadata = {
        "timestamp": timestamp,
        "suffix": suffix,
        "plot_type": "density_comparison" if args.compare_density else "single_variable",
        "density_reference_n": reference_n,
        "args": {
            "variable": args.variable,
            "values": args.values,
            "runs_per_value": args.runs_per_value,
            "seed": args.seed,
            "n": args.n,
            "m": args.m,
            "l": args.l,
            "rc": args.rc,
            "radius_min": args.radius_min,
            "radius_max": args.radius_max,
            "periodic": args.periodic,
            "target": args.target,
            "compare_density": args.compare_density,
        },
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(metadata, indent=2, sort_keys=True), encoding="utf-8")


def namespace_from_metadata(metadata: dict) -> argparse.Namespace:
    data = dict(metadata["args"])
    data.setdefault("show", False)
    data.setdefault("density_reference_n", metadata.get("density_reference_n"))
    return argparse.Namespace(**data)


def replot_saved(run_dir: Path) -> None:
    metadata_path = run_dir / "metadata.json"
    if not metadata_path.exists():
        raise SystemExit(f"No existe metadata.json en {run_dir}")

    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    suffix = metadata["suffix"]
    stem = run_dir / f"time_{suffix}_{metadata['timestamp']}"
    runs_csv = run_dir / f"{stem.name}_runs.csv"
    if not runs_csv.exists():
        raise SystemExit(f"No existe el CSV de corridas esperado: {runs_csv}")

    args = namespace_from_metadata(metadata)
    rows = read_csv(runs_csv)
    summaries = aggregate_by_series(rows)
    output_png = stem.with_suffix(".png")

    if metadata["plot_type"] == "density_comparison":
        reference_n = int(metadata["density_reference_n"])
        plot_density_comparison(
            output_png,
            args,
            summaries.get("free", []),
            summaries.get("fixed_density", []),
            reference_n,
        )
    else:
        plot_summary(output_png, args, summaries.get("standard", []))

    print(f"Grafico regenerado: {output_png}")


def with_series(summary: list[dict[str, float | int]], series: str) -> list[dict]:
    return [{"series": series, **row} for row in summary]


def main() -> None:
    args = parse_args()
    if args.replot_dir is not None:
        replot_saved(args.replot_dir)
        return

    if args.variable is None or args.values is None:
        raise SystemExit("--variable y --values son requeridos salvo que se use --replot-dir")
    if args.l <= 0 or args.rc <= 0 or args.radius_min <= 0 or args.radius_max <= 0:
        raise SystemExit("L, rc y radios deben ser mayores a 0")
    if args.radius_min > args.radius_max:
        raise SystemExit("radius-min debe ser menor o igual a radius-max")
    if args.compare_density and args.variable != "n":
        raise SystemExit("--compare-density solo se puede usar con --variable=n")
    if args.density_reference_n is not None and not args.compare_density:
        raise SystemExit("--density-reference-n requiere --compare-density")

    values = sorted(set(args.values))
    args.values = values
    reference_n = args.density_reference_n
    if args.compare_density:
        if reference_n is None:
            reference_n = values[len(values) // 2]
        if reference_n not in values:
            raise SystemExit("density-reference-n debe ser uno de los valores indicados en --values")
        if reference_n == values[-1]:
            raise SystemExit("density-reference-n debe dejar al menos un N mayor para incrementar N y L")

    if args.compile:
        compile_sources(args)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    suffix = "N_density_comparison" if args.compare_density else args.variable.upper()
    output_dir = Path("output/figures") / f"time_{suffix}_{timestamp}"
    stem = output_dir / f"time_{suffix}_{timestamp}"
    run_root = Path("output/analysis_runs") / f"time_{suffix}_{timestamp}"
    try:
        if args.compare_density:
            free_rows = run_analysis(args, run_root, series="free")
            fixed_rows = run_analysis(
                args,
                run_root,
                series="fixed_density",
                density_reference_n=reference_n,
            )
            free_summary = aggregate(free_rows)
            fixed_summary = aggregate(fixed_rows)
            rows = [*free_rows, *fixed_rows]
            summary_rows = [
                *with_series(free_summary, "free"),
                *with_series(fixed_summary, "fixed_density"),
            ]
            plot_density_comparison(
                stem.with_suffix(".png"),
                args,
                free_summary,
                fixed_summary,
                reference_n,
            )
        else:
            rows = run_analysis(args, run_root)
            summary = aggregate(rows)
            summary_rows = with_series(summary, "standard")
            plot_summary(stem.with_suffix(".png"), args, summary)

        runs_csv = Path(f"{stem}_runs.csv")
        summary_csv = Path(f"{stem}_summary.csv")
        metadata_json = output_dir / "metadata.json"
        write_csv(
            runs_csv,
            rows,
            ["series", "variable", "value", "run", "n", "m", "l", "density", "elapsed_ns", "elapsed_ms"],
        )
        write_csv(
            summary_csv,
            summary_rows,
            ["series", "value", "runs", "mean_ms", "stdev_ms", "stderr_ms"],
        )
        write_metadata(metadata_json, args, timestamp, suffix, reference_n)

        print(f"Carpeta: {output_dir}")
        print(f"Grafico: {stem.with_suffix('.png')}")
        print(f"Corridas: {runs_csv}")
        print(f"Resumen: {summary_csv}")
        print(f"Metadata: {metadata_json}")
    finally:
        if run_root.exists():
            shutil.rmtree(run_root)
        if run_root.parent.exists() and not any(run_root.parent.iterdir()):
            run_root.parent.rmdir()

    if args.show:
        plt.show()


if __name__ == "__main__":
    main()
