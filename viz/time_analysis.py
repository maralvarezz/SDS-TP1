#!/usr/bin/env python3
"""Run CIM timing analyses varying N or M and plot mean time with error bars.

Examples:
    python viz/time_analysis.py --variable m --values 3 4 5 6 7 8 9 10 --runs-per-value 10 --n 100
    python viz/time_analysis.py --variable n --values 100 200 300 400 --runs-per-value 10 --m 8
"""

from __future__ import annotations

import argparse
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
    parser.add_argument("--variable", choices=("n", "m"), required=True, help="Variable a barrer.")
    parser.add_argument("--values", nargs="+", type=positive_int, required=True, help="Valores de N o M a probar.")
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
        f"--l={args.l}",
        f"--rc={args.rc}",
        f"--radius-min={args.radius_min}",
        f"--radius-max={args.radius_max}",
        f"--periodic={str(args.periodic).lower()}",
        f"--random-seed={args.seed}",
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


def run_analysis(args: argparse.Namespace, run_root: Path) -> list[dict[str, float | int]]:
    rows: list[dict[str, float | int]] = []

    shared_m_input_paths = paths_for_value(run_root, "m_fixed_particles", args.n)
    base = java_base_args(args)

    for value in args.values:
        value_paths = paths_for_value(run_root, args.variable, value)
        if args.variable == "m":
            value_paths["static"] = shared_m_input_paths["static"]
            value_paths["dynamic"] = shared_m_input_paths["dynamic"]
        value_paths["outputs"].mkdir(parents=True, exist_ok=True)

        for run_number in range(1, args.runs_per_value + 1):
            n_value = args.n if args.variable == "m" else value
            m_value = value if args.variable == "m" else args.m
            input_mode = "random" if run_number == 1 and (args.variable == "n" or not value_paths["static"].exists()) else "file"

            cli_args = [
                *base,
                f"--n={n_value}",
                f"--m={m_value}",
                f"--input-mode={input_mode}",
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
                    "elapsed_ns": elapsed_ns,
                    "elapsed_ms": elapsed_ns / 1_000_000,
                }
            )
            print(f"{args.variable.upper()}={value} corrida {run_number}/{args.runs_per_value}: {elapsed_ns} ns")

    return rows


def aggregate(rows: list[dict[str, float | int]]) -> list[dict[str, float | int]]:
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


def plot_summary(path: Path, args: argparse.Namespace, summary: list[dict[str, float | int]]) -> None:
    x_values = [int(row["value"]) for row in summary]
    means = [float(row["mean_ms"]) for row in summary]
    errors = [float(row["stdev_ms"]) for row in summary]

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


def main() -> None:
    args = parse_args()
    if args.l <= 0 or args.rc <= 0 or args.radius_min <= 0 or args.radius_max <= 0:
        raise SystemExit("L, rc y radios deben ser mayores a 0")
    if args.radius_min > args.radius_max:
        raise SystemExit("radius-min debe ser menor o igual a radius-max")

    if args.compile:
        compile_sources(args)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    stem = Path("output/figures") / f"time_{args.variable.upper()}_{timestamp}"
    run_root = Path("output/analysis_runs") / f"time_{args.variable.upper()}_{timestamp}"
    try:
        rows = run_analysis(args, run_root)
        summary = aggregate(rows)

        plot_summary(stem.with_suffix(".png"), args, summary)

        print(f"Grafico: {stem.with_suffix('.png')}")
    finally:
        if run_root.exists():
            shutil.rmtree(run_root)
        if run_root.parent.exists() and not any(run_root.parent.iterdir()):
            run_root.parent.rmdir()

    if args.show:
        plt.show()


if __name__ == "__main__":
    main()
