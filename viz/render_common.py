"""Utilidades compartidas por plot_static.py e interactive_viewer.py.

Ambos scripts leen el mismo render_data.json que escribe Java
(RenderDataWriter) despues de correr el Cell Index Method, asi la lista de
vecinos que se dibuja es siempre la misma que calculo el algoritmo (no se
recalcula distancia borde-borde ni condicion de contorno periodica en
Python, para no arriesgar una inconsistencia entre lo que dice el TP y lo
que muestra el dibujo).
"""

import json

from palette import (
    COLOR_DEFAULT,
    COLOR_DEFAULT_EDGE,
    COLOR_NEIGHBOUR,
    COLOR_NEIGHBOUR_EDGE,
    COLOR_TARGET,
    COLOR_TARGET_EDGE,
    SURFACE,
    INK_PRIMARY,
    INK_SECONDARY,
    INK_MUTED,
    GRIDLINE,
    BASELINE,
)


def load_render_data(path):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    data["neighbours"] = {int(k): [int(v) for v in vs] for k, vs in data["neighbours"].items()}
    data["particles_by_id"] = {p["id"]: p for p in data["particles"]}
    return data


def style_axes(ax, l):
    ax.set_xlim(0, l)
    ax.set_ylim(0, l)
    ax.set_aspect("equal", adjustable="box")
    ax.set_facecolor(SURFACE)
    for spine in ("top", "right"):
        ax.spines[spine].set_visible(False)
    for spine in ("left", "bottom"):
        ax.spines[spine].set_color(BASELINE)
    ax.tick_params(colors=INK_MUTED, labelsize=9)
    ax.grid(True, color=GRIDLINE, linewidth=0.6, zorder=0)
    ax.set_xlabel("x", color=INK_SECONDARY, fontsize=10)
    ax.set_ylabel("y", color=INK_SECONDARY, fontsize=10)


def category_for(particle_id, target_id, neighbour_ids):
    if particle_id == target_id:
        return "target"
    if particle_id in neighbour_ids:
        return "neighbour"
    return "default"


CATEGORY_STYLE = {
    "default": {"face": COLOR_DEFAULT, "edge": COLOR_DEFAULT_EDGE, "label": "Particula", "zorder": 2},
    "neighbour": {"face": COLOR_NEIGHBOUR, "edge": COLOR_NEIGHBOUR_EDGE, "label": "Vecina (dist. borde-borde < rc)", "zorder": 3},
    "target": {"face": COLOR_TARGET, "edge": COLOR_TARGET_EDGE, "label": "Particula seleccionada", "zorder": 4},
}


def legend_handles(grouped):
    """PatchCollection no genera entradas de leyenda automaticamente
    (no implementa el protocolo de legend_handler para colecciones), asi que
    se arman handles 'proxy' (Patch sueltos, nunca agregados a los ejes) con
    el mismo estilo y la cuenta real de cada categoria."""
    from matplotlib.patches import Patch

    handles = []
    for category in ("target", "neighbour", "default"):
        points = grouped.get(category, [])
        if not points:
            continue
        style = CATEGORY_STYLE[category]
        handles.append(Patch(
            facecolor=style["face"],
            edgecolor=style["edge"],
            label=f'{style["label"]} ({len(points)})',
        ))
    return handles


def shell_offsets(x, y, shell_radius, l, periodic):
    """Offsets (dx, dy) para dibujar copias 'fantasma' de un circulo cuando,
    con condiciones periodicas, ese circulo se sale del area de simulacion
    por algun borde (asi se ve tambien la vecindad que 'da la vuelta')."""
    offsets = [(0.0, 0.0)]
    if not periodic:
        return offsets

    dxs = [0.0]
    dys = [0.0]
    if x - shell_radius < 0:
        dxs.append(l)
    if x + shell_radius > l:
        dxs.append(-l)
    if y - shell_radius < 0:
        dys.append(l)
    if y + shell_radius > l:
        dys.append(-l)

    seen = set()
    result = []
    for dx in dxs:
        for dy in dys:
            key = (dx, dy)
            if key not in seen:
                seen.add(key)
                result.append(key)
    return result
