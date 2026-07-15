package com.caliarena.domain.bracket

import com.caliarena.domain.match.Match

data class BracketOverview(
    val bracket: Bracket,
    val matches: List<Match>
)