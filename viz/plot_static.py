#!/usr/bin/env python3
"""Genera la figura pedida en el punto 1 del enunciado del TP1: todas las
particulas, la particula pasada como input de un color y sus vecinos de
otro. Se llama automaticamente desde Java (PlotInvoker) en cada corrida,
pero tambien se puede correr a mano:

    python3 viz/plot_static.py output/render_data.json output/figures/mi_figura.png
"""

import sys

import matplotlib

matplotlib.use("Agg")  # sin display: solo genera el archivo PNG
import matplotlib.pyplot as plt
from matplotlib.collections import PatchCollection
from matplotlib.patches import Circle

from palette import INK_PRIMARY
from render_common import (
    CATEGORY_STYLE,
    category_for,
    legend_handles,
    load_render_data,
    shell_offsets,
    style_axes,
)


def build_figure(data):
    l = data["l"]
    rc = data["rc"]
    periodic = data["periodic"]
    target_id = data["targetId"]
    neighbour_ids = set(data["neighbours"].get(target_id, []))
    particles = data["particles"]

    fig, ax = plt.subplots(figsize=(7, 7), dpi=150)
    style_axes(ax, l)

    grouped = {"default": [], "neighbour": [], "target": []}
    for p in particles:
        category = category_for(p["id"], target_id, neighbour_ids)
        grouped[category].append(p)

    for category in ("default", "neighbour", "target"):
        style = CATEGORY_STYLE[category]
        points = grouped[category]
        if not points:
            continue
        patches = [Circle((p["x"], p["y"]), p["radius"]) for p in points]
        collection = PatchCollection(
            patches,
            facecolor=style["face"],
            edgecolor=style["edge"],
            linewidth=0.6,
            zorder=style["zorder"],
            label=f'{style["label"]} ({len(points)})',
        )
        ax.add_collection(collection)

    target = data["particles_by_id"][target_id]
    shell_radius = target["radius"] + rc
    for dx, dy in shell_offsets(target["x"], target["y"], shell_radius, l, periodic):
        shell = Circle(
            (target["x"] + dx, target["y"] + dy),
            shell_radius,
            fill=False,
            linestyle="--",
            linewidth=1.4,
            edgecolor=CATEGORY_STYLE["target"]["edge"],
            zorder=5,
        )
        ax.add_patch(shell)

    ax.set_title(
        f"Cell Index Method — N={len(particles)}, L={l:g}, rc={rc:g}, "
        f"{'periodico' if periodic else 'no periodico'}\n"
        f"Particula {target_id}: {len(neighbour_ids)} vecinos  |  circulo punteado = radio rc",
        color=INK_PRIMARY,
        fontsize=11,
        loc="left",
    )

    legend = ax.legend(handles=legend_handles(grouped), loc="upper right", frameon=True, fontsize=9)
    legend.get_frame().set_edgecolor("#e1e0d9")
    legend.get_frame().set_facecolor("#fcfcfb")

    fig.tight_layout()
    return fig


def main():
    if len(sys.argv) != 3:
        print("Uso: plot_static.py <render_data.json> <salida.png>", file=sys.stderr)
        sys.exit(2)

    render_data_path, output_path = sys.argv[1], sys.argv[2]
    data = load_render_data(render_data_path)
    fig = build_figure(data)
    fig.savefig(output_path, facecolor=fig.get_facecolor())
    plt.close(fig)


if __name__ == "__main__":
    main()
