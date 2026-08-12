#!/usr/bin/env python3
"""Visualizacion dinamica para la demo en vivo (punto 2 del enunciado).

Abre una ventana con todas las particulas. Al hacer click sobre una
particula se ilumina su radio de interaccion rc: la particula clickeada
cambia de color, se le dibuja alrededor un circulo punteado de radio
(rc + su propio radio), y todas las particulas que esten a distancia
borde-borde menor a rc (ya calculadas por el Cell Index Method en Java, no
se recalculan aca) se pintan de otro color.

Uso:
    python3 viz/interactive_viewer.py output/render_data.json

Requiere un backend interactivo de matplotlib (TkAgg/MacOSX/Qt) — en Linux
sin entorno grafico no va a abrir ventana.
"""

import sys

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


class InteractiveViewer:
    def __init__(self, data):
        self.data = data
        self.l = data["l"]
        self.rc = data["rc"]
        self.periodic = data["periodic"]
        self.particles = data["particles"]
        self.selected_id = data["targetId"]

        self.fig, self.ax = plt.subplots(figsize=(7.5, 7.5))
        self.fig.canvas.manager.set_window_title("SDS TP1 - Cell Index Method (click para explorar rc)")
        self.fig.canvas.mpl_connect("button_press_event", self.on_click)
        self.draw()

    def nearest_particle(self, x, y):
        best_id, best_dist2 = None, None
        for p in self.particles:
            dist2 = (p["x"] - x) ** 2 + (p["y"] - y) ** 2
            if best_dist2 is None or dist2 < best_dist2:
                best_id, best_dist2 = p["id"], dist2
        return best_id

    def on_click(self, event):
        if event.inaxes != self.ax or event.xdata is None or event.ydata is None:
            return
        clicked_id = self.nearest_particle(event.xdata, event.ydata)
        if clicked_id is None:
            return
        self.selected_id = clicked_id
        self.draw()

    def draw(self):
        self.ax.clear()
        style_axes(self.ax, self.l)

        neighbour_ids = set(self.data["neighbours"].get(self.selected_id, []))
        grouped = {"default": [], "neighbour": [], "target": []}
        for p in self.particles:
            category = category_for(p["id"], self.selected_id, neighbour_ids)
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
            self.ax.add_collection(collection)

        target = self.data["particles_by_id"][self.selected_id]
        shell_radius = target["radius"] + self.rc
        for dx, dy in shell_offsets(target["x"], target["y"], shell_radius, self.l, self.periodic):
            shell = Circle(
                (target["x"] + dx, target["y"] + dy),
                shell_radius,
                fill=False,
                linestyle="--",
                linewidth=1.6,
                edgecolor=CATEGORY_STYLE["target"]["edge"],
                zorder=5,
            )
            self.ax.add_patch(shell)

        self.ax.set_title(
            f"Particula {self.selected_id}: {len(neighbour_ids)} vecinos (rc={self.rc:g})  —  "
            f"click en cualquier particula para explorarla\n"
            f"N={len(self.particles)}  L={self.l:g}  "
            f"{'periodico' if self.periodic else 'no periodico'}",
            color=INK_PRIMARY,
            fontsize=11,
            loc="left",
        )

        legend = self.ax.legend(handles=legend_handles(grouped), loc="upper right", frameon=True, fontsize=9)
        legend.get_frame().set_edgecolor("#e1e0d9")
        legend.get_frame().set_facecolor("#fcfcfb")

        self.fig.tight_layout()
        self.fig.canvas.draw_idle()

    def show(self):
        plt.show()


def main():
    if len(sys.argv) != 2:
        print("Uso: interactive_viewer.py <render_data.json>", file=sys.stderr)
        sys.exit(2)

    data = load_render_data(sys.argv[1])
    viewer = InteractiveViewer(data)
    viewer.show()


if __name__ == "__main__":
    main()
