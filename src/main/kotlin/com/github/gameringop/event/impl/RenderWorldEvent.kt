package com.github.gameringop.event.impl

import com.github.gameringop.event.Event
import com.github.gameringop.utils.render.RenderContext

class RenderWorldEvent(val ctx: RenderContext): Event(cancelable = false)
