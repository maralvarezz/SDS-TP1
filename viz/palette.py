"""Paleta categorica validada (skill dataviz) para las visualizaciones del TP1.

Se usan los primeros 3 slots del tema categorico por defecto porque un scatter
de particulas necesita separacion "all-pairs" (cualquier par de colores puede
terminar contiguo en el plano), y esos son los unicos 3 slots del tema que
pasan esa validacion completa (validado con scripts/validate_palette.js).
"""

# Superficie / tinta (modo claro)
SURFACE = "#fcfcfb"
INK_PRIMARY = "#0b0b0b"
INK_SECONDARY = "#52514e"
INK_MUTED = "#898781"
GRIDLINE = "#e1e0d9"
BASELINE = "#c3c2b7"

# Categorico (slot 1 / 2 / 3 del tema por defecto)
COLOR_DEFAULT = "#2a78d6"      # azul   - particulas sin relacion con la seleccionada
COLOR_DEFAULT_EDGE = "#184f95"

COLOR_NEIGHBOUR = "#eb6834"    # naranja - vecinos a distancia borde-borde < rc
COLOR_NEIGHBOUR_EDGE = "#a8431f"

COLOR_TARGET = "#1baf7a"       # verde/aqua - particula seleccionada
COLOR_TARGET_EDGE = "#0f7a53"

FONT_FAMILY = "DejaVu Sans"  # equivalente disponible localmente a system-ui/sans-serif
