package com.feuerschvenger.perlinsedge.app;

import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxCamera;
import com.feuerschvenger.perlinsedge.infra.fx.graphics.FxRenderer;
import com.feuerschvenger.perlinsedge.infra.fx.input.FxInputHandler;
import com.feuerschvenger.perlinsedge.ui.view.FxMinimapDrawer;
import javafx.scene.canvas.Canvas;

/**
 * The drawing and input surface of the game: what is drawn on the main canvas and where the pointer and the keys come
 * from. Grouping them keeps the constructors of the coordinators short.
 *
 * @param renderer      canvas renderer
 * @param camera        camera used to convert screen to tile coordinates
 * @param inputHandler  keyboard and scroll input source
 * @param minimapDrawer minimap view
 * @param mainCanvas    main game canvas, which receives the game clicks
 */
public record GameView(FxRenderer renderer, FxCamera camera, FxInputHandler inputHandler,
                       FxMinimapDrawer minimapDrawer, Canvas mainCanvas) {
}
