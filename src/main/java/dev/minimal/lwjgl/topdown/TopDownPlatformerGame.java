package dev.minimal.lwjgl.topdown;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBEasyFont;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

final class TopDownPlatformerGame {
    private static final String OBJECTIVE_TEXT = "Reach the exit (G) without touching hazards (X). WASD / Arrow keys move. Press R to restart.";
    private static final int PLAYER_SQUAD_SIZE = 4;
    private static final int ENEMY_SQUAD_SIZE = 4;
    private static final int MOVEMENT_RANGE = 4;

    private final ByteBuffer textBuffer = BufferUtils.createByteBuffer(64 * 1024);

    private Level level;
    private CursorController cursor;
    private Squad playerSquad;
    private Squad enemySquad;
    private PlacementController placementController;
    private MovementPreview movementPreview;
    private WeaponMenu weaponMenu;
    private Weapon equippedWeapon;
    private boolean partyReviewed;
    private Unit loadoutTarget;
    private final Set<String> assignedLoadouts = new HashSet<>();
    private boolean loadoutChosen;
    private BattlePhase battlePhase = BattlePhase.PLACEMENT;
    private boolean placementBannerVisible = true;
    private int selectedUnitIndex = -1;
    private GameStatus status = GameStatus.RUNNING;
    private String contextualMessage = OBJECTIVE_TEXT;
    private String messageBeforeMenu = OBJECTIVE_TEXT;
    private long window = NULL;
    private boolean regenerateOnReset = false;
    private UiMode uiMode = UiMode.CONFIG;
    private LevelSettings currentSettings;
    private LevelSettings editingSettings;
    private final StringBuilder seedInput = new StringBuilder();
    private Unit lastSelectedUnit;

    TopDownPlatformerGame(GameConfig config) {
        this.currentSettings = LevelSettings.fromConfig(config);
        this.editingSettings = this.currentSettings;
        this.weaponMenu = new WeaponMenu(WeaponDefinitions.catalog());
        this.equippedWeapon = weaponMenu.selected();
    }

    private boolean handleUnitSelectionKey(int key) {
        if (playerSquad == null || playerSquad.units().isEmpty()) {
            return false;
        }
        if (key == GLFW_KEY_Q) {
            selectNextUnit(-1);
            return true;
        }
        if (key == GLFW_KEY_E) {
            selectNextUnit(1);
            return true;
        }
        return false;
    }

    private boolean handleUnitActionKey(int key) {
        if (key != GLFW_KEY_ENTER && key != GLFW_KEY_SPACE) {
            return false;
        }
        if (selectUnitAtCursor()) {
            return true;
        }
        if (selectedUnitIndex >= 0 && attemptUnitMove()) {
            return true;
        }
        contextualMessage = "Move the cursor onto a friendly unit and press Enter to select it.";
        return true;
    }

    void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        loadNewLevel();

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(level.pixelWidth(), level.pixelHeight(), "Isometric Hazard Run", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Failed to create GLFW window");
        }

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                handleKeyPress(key);
            }
        });
        glfwSetFramebufferSizeCallback(window, (win, width, height) -> glViewport(0, 0, width, height));

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        GL.createCapabilities();
        glDisable(GL_DEPTH_TEST);
        glClearColor(0.08f, 0.08f, 0.1f, 1.0f);
        enterConfigMode();
    }

    private void loop() {
        double lastTime = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float delta = (float) (now - lastTime);
            lastTime = now;

            update(delta);
            render();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void update(float deltaSeconds) {
        if (uiMode == UiMode.CONFIG || uiMode == UiMode.WEAPON_MENU || uiMode == UiMode.PARTY_MENU
            || battlePhase != BattlePhase.ACTIVE) {
            return;
        }
        // Movement/turn logic will be introduced in a future iteration.
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT);
        glViewport(0, 0, level.pixelWidth(), level.pixelHeight());

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, level.pixelWidth(), level.pixelHeight(), 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        level.render();
        renderSpawnZones();
        renderReachablePreview();
        renderUnits();
        if (cursor != null) {
            cursor.render();
        }
        drawHud();
        if (uiMode == UiMode.CONFIG) {
            drawConfigOverlay();
        } else if (uiMode == UiMode.PARTY_MENU) {
            drawPartyMenu();
        } else if (uiMode == UiMode.WEAPON_MENU) {
            drawWeaponMenu();
        } else if (battlePhase == BattlePhase.PLACEMENT && placementBannerVisible) {
            drawLandingBanner();
        }
    }

    private void handleKeyPress(int key) {
        if (uiMode == UiMode.PARTY_MENU) {
            handlePartyMenuKey(key);
            return;
        }
        if (uiMode == UiMode.WEAPON_MENU) {
            handleWeaponMenuKey(key);
            return;
        }
        if (key == GLFW_KEY_ESCAPE) {
            glfwSetWindowShouldClose(window, true);
            return;
        }
        if (uiMode == UiMode.CONFIG) {
            handleConfigKey(key);
            return;
        }
        if (key == GLFW_KEY_P) {
            openPartyMenu();
            return;
        }
        if (key == GLFW_KEY_V) {
            openWeaponMenu();
            return;
        }
        if (key == GLFW_KEY_TAB || key == GLFW_KEY_M) {
            enterConfigMode();
            return;
        }
        if (key == GLFW_KEY_R) {
            boolean regenerateLevel = regenerateOnReset || status == GameStatus.DEAD;
            resetLevel(regenerateLevel);
            return;
        }
        if (battlePhase == BattlePhase.PLACEMENT) {
            handlePlacementInput(key);
            return;
        }
        if (battlePhase == BattlePhase.ACTIVE && handleUnitSelectionKey(key)) {
            return;
        }
        if (battlePhase == BattlePhase.ACTIVE && handleUnitActionKey(key)) {
            return;
        }
        if (status != GameStatus.RUNNING) {
            return;
        }
        Direction direction = Direction.fromKey(key);
        if (direction != null && cursor != null) {
            cursor.move(direction);
        }
    }

    private void handlePlacementInput(int key) {
        if (cursor == null || placementController == null) {
            return;
        }
        Direction direction = Direction.fromKey(key);
        if (direction != null) {
            cursor.move(direction);
            return;
        }
        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
            if (placementBannerVisible) {
                placementBannerVisible = false;
                contextualMessage = String.format(Locale.ROOT, "Place %d units on blue tiles.", placementController.remainingUnits());
                return;
            }
            PlacementController.PlacementResult result = placementController.attemptPlacement();
            if (result == PlacementController.PlacementResult.INVALID) {
                contextualMessage = "Select an empty blue spawn tile and press Enter to deploy.";
                return;
            }
            if (result == PlacementController.PlacementResult.PLACED) {
                contextualMessage = String.format(Locale.ROOT, "Unit deployed. %d remaining.",
                    placementController.remainingUnits());
                return;
            }
            contextualMessage = "All units deployed! Engage when ready.";
            enterActivePhase();
        }
    }

    private void handleConfigKey(int key) {
        switch (key) {
            case GLFW_KEY_TAB, GLFW_KEY_M -> exitConfigModeWithoutApply();
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> applyConfigChanges();
            case GLFW_KEY_LEFT -> editingSettings = editingSettings.withWidthDelta(-2);
            case GLFW_KEY_RIGHT -> editingSettings = editingSettings.withWidthDelta(2);
            case GLFW_KEY_DOWN -> editingSettings = editingSettings.withHeightDelta(-2);
            case GLFW_KEY_UP -> editingSettings = editingSettings.withHeightDelta(2);
            case GLFW_KEY_COMMA -> editingSettings = editingSettings.withTileSizeDelta(-2);
            case GLFW_KEY_PERIOD -> editingSettings = editingSettings.withTileSizeDelta(2);
            case GLFW_KEY_LEFT_BRACKET -> editingSettings = editingSettings.withHazardDelta(-0.01f);
            case GLFW_KEY_RIGHT_BRACKET -> editingSettings = editingSettings.withHazardDelta(0.01f);
            case GLFW_KEY_N -> {
                editingSettings = editingSettings.withRandomSeed();
                seedInput.setLength(0);
            }
            case GLFW_KEY_L -> editingSettings = editingSettings.withSeedLock(!editingSettings.lockSeed());
            case GLFW_KEY_BACKSPACE -> {
                if (seedInput.length() > 0) {
                    seedInput.setLength(seedInput.length() - 1);
                }
            }
            case GLFW_KEY_DELETE -> seedInput.setLength(0);
            default -> appendSeedCharacter(key);
        }
    }

    private void handlePartyMenuKey(int key) {
        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_TAB || key == GLFW_KEY_M || key == GLFW_KEY_P) {
            exitPartyMenu();
            return;
        }
        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER || key == GLFW_KEY_SPACE) {
            exitPartyMenu();
        }
    }

    private void handleWeaponMenuKey(int key) {
        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_TAB || key == GLFW_KEY_M || key == GLFW_KEY_V) {
            exitWeaponMenu();
            return;
        }
        if (weaponMenu == null) {
            exitWeaponMenu();
            return;
        }
        if (key == GLFW_KEY_UP || key == GLFW_KEY_W) {
            weaponMenu.moveSelection(-1);
            return;
        }
        if (key == GLFW_KEY_DOWN || key == GLFW_KEY_S) {
            weaponMenu.moveSelection(1);
            return;
        }
        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER || key == GLFW_KEY_SPACE) {
            Weapon selection = weaponMenu.selected();
            if (selection != null) {
                equippedWeapon = selection;
                if (loadoutTarget != null) {
                    loadoutTarget.equipWeapon(selection);
                    assignedLoadouts.add(loadoutTarget.id());
                    contextualMessage = String.format(Locale.ROOT, "Equipped [%s] for %s.", selection.displayName(),
                        loadoutTarget.displayName());
                } else {
                    contextualMessage = String.format(Locale.ROOT, "Equipped [%s] [%s].", selection.displayName(),
                        selection.tag().symbol());
                }
                loadoutTarget = null;
            }
            exitWeaponMenu();
        }
    }

    private void enterConfigMode() {
        uiMode = UiMode.CONFIG;
        editingSettings = currentSettings;
        seedInput.setLength(0);
        contextualMessage = "Config mode: adjust parameters then press Enter to apply.";
    }

    private void exitConfigModeWithoutApply() {
        seedInput.setLength(0);
        if (battlePhase == BattlePhase.PLACEMENT) {
            contextualMessage = "Press Enter to acknowledge the briefing, then deploy your squad.";
        } else {
            contextualMessage = OBJECTIVE_TEXT;
        }
        ensureWeaponMenuForPlacement();
    }

    private void applyConfigChanges() {
        if (seedInput.length() > 0) {
            try {
                long parsed = Long.parseLong(seedInput.toString());
                editingSettings = editingSettings.withSeed(parsed);
            } catch (NumberFormatException ignored) {
            }
            seedInput.setLength(0);
        }
        currentSettings = editingSettings;
        loadNewLevel();
        status = GameStatus.RUNNING;
        regenerateOnReset = false;
        ensureWeaponMenuForPlacement();
    }

    private void openPartyMenu() {
        if (playerSquad == null) {
            return;
        }
        if (uiMode != UiMode.PARTY_MENU) {
            uiMode = UiMode.PARTY_MENU;
            messageBeforeMenu = contextualMessage;
            contextualMessage = "Party roster: press P/Tab to close, Enter to continue.";
        }
    }

    private void openWeaponMenu() {
        if (battlePhase == BattlePhase.PLACEMENT && !partyReviewed) {
            openPartyMenu();
            return;
        }
        loadoutTarget = determineNextLoadoutTarget();
        if (weaponMenu == null || weaponMenu.weapons().isEmpty()) {
            weaponMenu = new WeaponMenu(WeaponDefinitions.catalog());
        }
        if (uiMode != UiMode.WEAPON_MENU) {
            uiMode = UiMode.WEAPON_MENU;
            messageBeforeMenu = contextualMessage;
            String hint = loadoutTarget != null ? String.format(" (assigning %s)", loadoutTarget.displayName()) : "";
            contextualMessage = "Weapon menu" + hint + ": Up/Down to move, Enter equips, V/Tab closes.";
        }
    }

    private Unit determineNextLoadoutTarget() {
        if (playerSquad != null) {
            List<Unit> units = playerSquad.units();
            if (lastSelectedUnit != null && lastSelectedUnit.isPlaced() && units.contains(lastSelectedUnit)) {
                return lastSelectedUnit;
            }
            if (selectedUnitIndex >= 0 && selectedUnitIndex < units.size()) {
                Unit selected = units.get(selectedUnitIndex);
                if (selected != null && selected.isPlaced()) {
                    return selected;
                }
            }
        }
        if (placementController != null) {
            Unit next = placementController.nextUnit();
            if (next != null && !assignedLoadouts.contains(next.id())) {
                return next;
            }
        }
        if (playerSquad != null) {
            for (Unit unit : playerSquad.units()) {
                if (!assignedLoadouts.contains(unit.id())) {
                    return unit;
                }
            }
            if (!playerSquad.units().isEmpty()) {
                return playerSquad.units().get(0);
            }
        }
        return null;
    }

    private boolean loadoutsComplete() {
        if (playerSquad == null || playerSquad.units().isEmpty()) {
            return false;
        }
        for (Unit unit : playerSquad.units()) {
            if (!assignedLoadouts.contains(unit.id())) {
                return false;
            }
        }
        return true;
    }

    private Unit loadoutPreviewUnit() {
        if (battlePhase == BattlePhase.ACTIVE && selectedUnitIndex >= 0 && playerSquad != null) {
            List<Unit> units = playerSquad.units();
            if (selectedUnitIndex < units.size()) {
                return units.get(selectedUnitIndex);
            }
        }
        if (placementController != null) {
            Unit next = placementController.nextUnit();
            if (next != null) {
                return next;
            }
        }
        if (playerSquad != null && !playerSquad.units().isEmpty()) {
            return playerSquad.units().get(0);
        }
        return null;
    }

    private void exitPartyMenu() {
        uiMode = UiMode.PLAYING;
        partyReviewed = true;
        if (contextualMessage != null && contextualMessage.startsWith("Party roster") && messageBeforeMenu != null) {
            contextualMessage = messageBeforeMenu;
        }
        ensureMenusForPlacement();
    }

    private void exitWeaponMenu() {
        uiMode = UiMode.PLAYING;
        loadoutChosen = loadoutsComplete();
        if (!loadoutChosen) {
            ensureMenusForPlacement();
            return;
        }
        if (contextualMessage != null && contextualMessage.startsWith("Weapon menu") && messageBeforeMenu != null) {
            contextualMessage = messageBeforeMenu;
        }
    }

    private void ensureWeaponMenuForPlacement() {
        ensureMenusForPlacement();
    }

    private void ensureMenusForPlacement() {
        if (battlePhase != BattlePhase.PLACEMENT) {
            uiMode = UiMode.PLAYING;
            return;
        }
        if (!partyReviewed) {
            openPartyMenu();
            return;
        }
        if (!loadoutChosen) {
            openWeaponMenu();
            return;
        }
        uiMode = UiMode.PLAYING;
    }

    private boolean appendSeedCharacter(int key) {
        char digit = digitForKey(key);
        if (digit != 0) {
            seedInput.append(digit);
            return true;
        }
        if ((key == GLFW_KEY_MINUS || key == GLFW_KEY_KP_SUBTRACT) && seedInput.length() == 0) {
            seedInput.append('-');
            return true;
        }
        return false;
    }

    private char digitForKey(int key) {
        if (key >= GLFW_KEY_0 && key <= GLFW_KEY_9) {
            return (char) ('0' + (key - GLFW_KEY_0));
        }
        if (key >= GLFW_KEY_KP_0 && key <= GLFW_KEY_KP_9) {
            return (char) ('0' + (key - GLFW_KEY_KP_0));
        }
        return 0;
    }

    private void loadNewLevel() {
        LevelGenerator generator = new LevelGenerator(
            currentSettings.width(),
            currentSettings.height(),
            currentSettings.tileSize(),
            currentSettings.hazardDensity(),
            currentSettings.seed()
        );
        level = generator.generate();
        if (cursor == null) {
            cursor = new CursorController(level);
        } else {
            cursor.attach(level);
        }
        initializeBattleState();
        if (window != NULL) {
            glfwSetWindowSize(window, level.pixelWidth(), level.pixelHeight());
        }
        editingSettings = currentSettings;
        ensureMenusForPlacement();
    }

    private void resetLevel(boolean regenerateLevel) {
        status = GameStatus.RUNNING;
        if (regenerateLevel || level == null) {
            if (!currentSettings.lockSeed()) {
                currentSettings = currentSettings.withRandomSeed();
            }
            loadNewLevel();
        } else {
            if (cursor != null) {
                cursor.attach(level);
            }
            initializeBattleState();
        }
        regenerateOnReset = false;
        ensureWeaponMenuForPlacement();
    }

    private void drawHud() {
        float hudHeight = 80f;

        glColor4f(0f, 0f, 0f, 0.65f);
        glBegin(GL_QUADS);
        glVertex2f(0f, 0f);
        glVertex2f(level.pixelWidth(), 0f);
        glVertex2f(level.pixelWidth(), hudHeight);
        glVertex2f(0f, hudHeight);
        glEnd();

        if (placementBannerVisible && battlePhase == BattlePhase.PLACEMENT) {
            drawText(12f, 28f, "Press Enter to dismiss the tactical briefing.", 0.95f, 0.88f, 0.72f);
        } else {
            drawText(12f, 20f, OBJECTIVE_TEXT, 1f, 1f, 1f);
            drawText(12f, 42f, contextualMessage, 0.85f, 0.95f, 1f);
        }
        Unit previewUnit = loadoutPreviewUnit();
        Weapon loadoutWeapon = previewUnit != null ? previewUnit.weapon() : equippedWeapon;
        String loadout;
        if (loadoutWeapon == null) {
            loadout = "Loadout: press V to open the weapon menu.";
        } else {
            String unitSuffix = previewUnit != null ? " for " + previewUnit.displayName() : "";
            loadout = String.format(Locale.ROOT, "Loadout%s: [%s] - %s  DMG %d / DEF %d (V to change)",
                unitSuffix, loadoutWeapon.tag().symbol(), loadoutWeapon.displayName(), loadoutWeapon.damage(),
                loadoutWeapon.defense());
        }
        drawText(12f, 64f, loadout, 0.82f, 0.92f, 0.98f);
    }

    private void drawConfigOverlay() {
        glColor4f(0f, 0f, 0f, 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(0f, 0f);
        glVertex2f(level.pixelWidth(), 0f);
        glVertex2f(level.pixelWidth(), level.pixelHeight());
        glVertex2f(0f, level.pixelHeight());
        glEnd();

        float y = 60f;
        drawText(24f, y, "CONFIG MODE - Enter to apply, Tab/M to cancel", 1f, 0.95f, 0.75f);
        y += 32f;
        y = drawConfigRow(y, "Width (Left/Right)", editingSettings.width() + " tiles");
        y = drawConfigRow(y, "Height (Up/Down)", editingSettings.height() + " tiles");
        y = drawConfigRow(y, "Tile size (,/.)", editingSettings.tileSize() + " px");
        y = drawConfigRow(y, "Hazard ([/])", Math.round(editingSettings.hazardDensity() * 100f) + "%");

        String seedMode = editingSettings.lockSeed() ? "locked" : "auto-refresh";
        y = drawConfigRow(y, "Seed value", editingSettings.seed() + " (" + seedMode + ")");
        String buffer = seedInput.length() > 0 ? seedInput.toString() : "<type digits / '-' to override>";
        y = drawConfigRow(y, "Seed input", buffer);

        y += 10f;
        drawText(24f, y, "[N] random seed    [L] toggle lock", 0.82f, 0.88f, 0.92f);
        y += 20f;
        drawText(24f, y, "Press R in-game to restart, Tab to reopen this menu anytime.", 0.8f, 0.8f, 0.8f);
    }

    private void drawLandingBanner() {
        if (level == null) {
            return;
        }
        float panelWidth = Math.min(level.pixelWidth() - 80f, 760f);
        float panelHeight = 190f;
        float originX = (level.pixelWidth() - panelWidth) / 2f;
        float originY = level.pixelHeight() * 0.3f;

        glColor4f(0f, 0f, 0f, 0.78f);
        glBegin(GL_QUADS);
        glVertex2f(originX, originY);
        glVertex2f(originX + panelWidth, originY);
        glVertex2f(originX + panelWidth, originY + panelHeight);
        glVertex2f(originX, originY + panelHeight);
        glEnd();

        float x = originX + 36f;
        float y = originY + 52f;
        drawText(x, y, "ISOMETRIC SRPG DEPLOYMENT", 0.95f, 0.88f, 0.72f, 1.2f);
        y += 36f;
        drawText(x, y, "Drag the cursor across luminous blue tiles and press Enter to drop each unit.", 0.85f, 0.9f, 1f, 0.95f);
        y += 26f;
        drawText(x, y, "Four operatives must be placed before we can advance on the enemy squad.", 0.82f, 0.92f, 0.88f, 0.95f);
        y += 32f;

        drawText(x, y, "Tips:", 0.92f, 0.6f, 0.6f, 1.05f);
        y += 24f;
        drawText(x, y, "- WASD / Arrow keys shift the tactical cursor", 0.9f, 0.85f, 1f, 0.95f);
        y += 22f;
        drawText(x, y, "- Enter confirms placement on an unoccupied spawn tile", 0.9f, 0.85f, 1f, 0.95f);
    }

    private void drawPartyMenu() {
        if (level == null || playerSquad == null) {
            return;
        }
        List<Unit> units = playerSquad.units();
        float panelWidth = Math.min(level.pixelWidth() - 120f, 620f);
        float rowHeight = 28f;
        float panelHeight = 120f + units.size() * rowHeight;
        float originX = (level.pixelWidth() - panelWidth) / 2f;
        float originY = Math.max(32f, level.pixelHeight() * 0.18f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glColor4f(0f, 0f, 0f, 0.82f);
        glBegin(GL_QUADS);
        glVertex2f(originX, originY);
        glVertex2f(originX + panelWidth, originY);
        glVertex2f(originX + panelWidth, originY + panelHeight);
        glVertex2f(originX, originY + panelHeight);
        glEnd();

        glColor4f(0.35f, 0.9f, 0.65f, 0.2f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(originX, originY);
        glVertex2f(originX + panelWidth, originY);
        glVertex2f(originX + panelWidth, originY + panelHeight);
        glVertex2f(originX, originY + panelHeight);
        glEnd();

        float textX = originX + 28f;
        float y = originY + 44f;
        drawText(textX, y, "PARTY ROSTER", 0.9f, 0.98f, 0.86f, 1.1f);
        y += 28f;
        drawText(textX, y, "Review squad stats before deployment. P/Tab closes, Enter continues.", 0.82f, 0.9f, 0.96f);
        y += 18f;

        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            float rowY = y + i * rowHeight;
            if ((i % 2) == 0) {
                glColor4f(0.2f, 0.65f, 0.45f, 0.2f);
                glBegin(GL_QUADS);
                glVertex2f(originX + 16f, rowY - 6f);
                glVertex2f(originX + panelWidth - 16f, rowY - 6f);
                glVertex2f(originX + panelWidth - 16f, rowY + rowHeight - 10f);
                glVertex2f(originX + 16f, rowY + rowHeight - 10f);
                glEnd();
            }
            Weapon weapon = unit.weapon();
            String weaponLabel = weapon == null
                ? "unarmed"
                : String.format(Locale.ROOT, "[%s] %s DMG %d DEF %d", weapon.tag().symbol(), weapon.displayName(),
                weapon.damage(), weapon.defense());
            String label = String.format(Locale.ROOT, "%s   HP %d/%d   SP %d/%d   Weapon %s",
                unit.displayName(), unit.hp(), unit.maxHp(), unit.sp(), unit.maxSp(), weaponLabel);
            drawText(textX, rowY, label, 0.9f, 0.95f, 0.92f);
        }

        glDisable(GL_BLEND);
    }

    private void drawWeaponMenu() {
        if (level == null || weaponMenu == null) {
            return;
        }
        List<Weapon> weapons = weaponMenu.weapons();
        if (weapons.isEmpty()) {
            return;
        }
        float panelWidth = Math.min(level.pixelWidth() - 120f, 640f);
        float rowHeight = 44f;
        float panelHeight = 140f + weapons.size() * rowHeight;
        float originX = (level.pixelWidth() - panelWidth) / 2f;
        float originY = Math.max(32f, level.pixelHeight() * 0.18f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glColor4f(0f, 0f, 0f, 0.82f);
        glBegin(GL_QUADS);
        glVertex2f(originX, originY);
        glVertex2f(originX + panelWidth, originY);
        glVertex2f(originX + panelWidth, originY + panelHeight);
        glVertex2f(originX, originY + panelHeight);
        glEnd();

        glColor4f(0.35f, 0.75f, 1f, 0.18f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(originX, originY);
        glVertex2f(originX + panelWidth, originY);
        glVertex2f(originX + panelWidth, originY + panelHeight);
        glVertex2f(originX, originY + panelHeight);
        glEnd();

        float textX = originX + 28f;
        float y = originY + 44f;
        drawText(textX, y, "WEAPON LOADOUT", 0.95f, 0.88f, 0.72f, 1.1f);
        y += 28f;
        drawText(textX, y, "Up/Down: move cursor    Enter: equip    V/Tab: close", 0.82f, 0.9f, 0.96f);
        y += 18f;
        String assignmentLine = loadoutTarget != null
            ? String.format(Locale.ROOT, "Assigning to %s - press Enter to equip.", loadoutTarget.displayName())
            : "No weapon assigned yet — pick a unit before choosing a loadout.";
        drawText(textX, y, assignmentLine, 0.78f, 0.88f, 1f);
        y += 18f;

        int selectedIndex = weaponMenu.selectedIndex();
        for (int i = 0; i < weapons.size(); i++) {
            Weapon weapon = weapons.get(i);
            float rowY = y + i * rowHeight;
            boolean selected = i == selectedIndex;
            if (selected) {
                glColor4f(0.2f, 0.55f, 0.9f, 0.25f);
                glBegin(GL_QUADS);
                glVertex2f(originX + 16f, rowY - 6f);
                glVertex2f(originX + panelWidth - 16f, rowY - 6f);
                glVertex2f(originX + panelWidth - 16f, rowY + rowHeight - 10f);
                glVertex2f(originX + 16f, rowY + rowHeight - 10f);
                glEnd();
            }
            drawText(textX, rowY, selected ? ">" : " ", 0.9f, 0.9f, 0.9f);
            String label = String.format(Locale.ROOT, "[%s] - %s   DMG %d / DEF %d",
                weapon.tag().symbol(), weapon.displayName(), weapon.damage(), weapon.defense());
            float labelR = selected ? 0.98f : 0.9f;
            float labelG = selected ? 0.98f : 0.9f;
            float labelB = selected ? 1f : 0.94f;
            drawText(textX + 18f, rowY, label, labelR, labelG, labelB);
            String description = weapon.description();
            if (description != null && !description.isBlank()) {
                drawText(textX + 18f, rowY + 16f, description, 0.6f, 0.75f, 0.92f, 0.82f);
            }
        }

        if (equippedWeapon != null) {
            String equipped = String.format(Locale.ROOT, "Equipped: [%s] - %s   DMG %d / DEF %d",
                equippedWeapon.tag().symbol(), equippedWeapon.displayName(), equippedWeapon.damage(),
                equippedWeapon.defense());
            drawText(textX, originY + panelHeight - 32f, equipped, 0.88f, 0.95f, 0.98f);
        }

        glDisable(GL_BLEND);
    }

    private float drawConfigRow(float y, String label, String value) {
        float labelX = 24f;
        float valueX = labelX + 260f;
        drawText(labelX, y, label + ":", 0.84f, 0.9f, 1f);
        drawText(valueX, y, value, 0.95f, 0.95f, 0.9f);
        return y + 26f;
    }

    private void selectNextUnit(int delta) {
        if (playerSquad == null || playerSquad.units().isEmpty()) {
            selectedUnitIndex = -1;
            return;
        }
        int size = playerSquad.units().size();
        int start = selectedUnitIndex >= 0 ? selectedUnitIndex : 0;
        for (int i = 1; i <= size; i++) {
            int candidate = Math.floorMod(start + delta * i, size);
            Unit unit = playerSquad.units().get(candidate);
            if (unit.isPlaced()) {
                selectUnit(candidate);
                break;
            }
        }
    }

    private void selectUnit(int index) {
        selectUnit(index, true);
    }

    private void selectUnit(int index, boolean syncCursor) {
        if (playerSquad == null || cursor == null || index < 0 || index >= playerSquad.units().size()) {
            selectedUnitIndex = -1;
            movementPreview = null;
            contextualMessage = "No unit available to control.";
            return;
        }
        Unit unit = playerSquad.units().get(index);
        if (!unit.isPlaced()) {
            selectedUnitIndex = -1;
            movementPreview = null;
            contextualMessage = "Unit not deployed.";
            return;
        }
        if (unit.hasMovedThisTurn()) {
            contextualMessage = String.format(Locale.ROOT, "%s already moved this turn.", unit.displayName());
            return;
        }
        selectedUnitIndex = index;
        if (syncCursor) {
            cursor.moveTo(unit.position());
        }
        movementPreview = MovementPreview.calculate(
            level,
            unit,
            playerSquad != null ? playerSquad.units() : List.of(),
            enemySquad != null ? enemySquad.units() : List.of(),
            MOVEMENT_RANGE
        );
        lastSelectedUnit = unit;
        contextualMessage = String.format(Locale.ROOT, "Selected %s - press Q/E to cycle units.", unit.displayName());
    }

    private void renderSpawnZones() {
        if (level == null) {
            return;
        }
        for (GridPosition spawn : level.playerSpawnTiles()) {
            IsoTileHighlightRenderer.draw(
                level,
                spawn.col(),
                spawn.row(),
                0.25f,
                0.55f,
                0.95f,
                0.15f,
                0.5f,
                0.85f,
                1f,
                0.6f
            );
        }
        for (GridPosition spawn : level.enemySpawnTiles()) {
            IsoTileHighlightRenderer.draw(
                level,
                spawn.col(),
                spawn.row(),
                0.85f,
                0.35f,
                0.35f,
                0.18f,
                0.95f,
                0.55f,
                0.45f,
                0.75f
            );
        }
    }

    private void renderUnits() {
        if (level == null) {
            return;
        }
        renderSquadUnits(enemySquad);
        renderSquadUnits(playerSquad);
        highlightSelectedUnit();
    }

    private void renderSquadUnits(Squad squad) {
        if (squad == null) {
            return;
        }
        for (Unit unit : squad.units()) {
            UnitRenderer.draw(level, unit);
        }
    }

    private void highlightSelectedUnit() {
        if (level == null || playerSquad == null || selectedUnitIndex < 0) {
            return;
        }
        List<Unit> units = playerSquad.units();
        if (selectedUnitIndex >= units.size()) {
            return;
        }
        Unit selected = units.get(selectedUnitIndex);
        if (selected == null || !selected.isPlaced()) {
            return;
        }
        GridPosition pos = selected.position();
        IsoTileHighlightRenderer.draw(
            level,
            pos.col(),
            pos.row(),
            0.3f,
            0.75f,
            0.95f,
            0.4f,
            0.9f,
            1f,
            1f,
            0.95f
        );
    }

    private boolean selectUnitAtCursor() {
        if (cursor == null || playerSquad == null) {
            return false;
        }
        GridPosition focus = cursor.gridPosition();
        List<Unit> units = playerSquad.units();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (unit.isPlaced() && unit.position().equals(focus)) {
                selectUnit(i, false);
                contextualMessage = String.format(Locale.ROOT, "Selected %s - press Q/E to cycle units.", unit.displayName());
                return true;
            }
        }
        return false;
    }

    private boolean attemptUnitMove() {
        if (cursor == null || playerSquad == null || movementPreview == null || selectedUnitIndex < 0) {
            return false;
        }
        GridPosition target = cursor.gridPosition();
        if (!movementPreview.isReachable(target)) {
            contextualMessage = "Cannot move there — tile not in range.";
            return true;
        }
        Unit unit = playerSquad.units().get(selectedUnitIndex);
        if (unit == null || !unit.isPlaced()) {
            contextualMessage = "Unit not available for movement.";
            return true;
        }
        if (unit.hasMovedThisTurn()) {
            contextualMessage = String.format(Locale.ROOT, "%s already moved this turn.", unit.displayName());
            return true;
        }
        unit.placeAt(target);
        unit.setMovedThisTurn(true);
        contextualMessage = String.format(Locale.ROOT, "%s moved to (%d,%d).", unit.displayName(), target.col(), target.row());
        selectedUnitIndex = -1;
        movementPreview = null;
        if (allUnitsMoved(playerSquad)) {
            executeEnemyTurn();
            resetMovementFlags(playerSquad);
            contextualMessage = "Enemy units repositioned. Player phase refreshed.";
        }
        return true;
    }

    private void renderReachablePreview() {
        if (level == null || battlePhase != BattlePhase.ACTIVE || movementPreview == null || playerSquad == null || selectedUnitIndex < 0) {
            return;
        }
        Unit selected = playerSquad.units().get(selectedUnitIndex);
        if (selected == null || !selected.isPlaced()) {
            return;
        }
        List<GridPosition> tiles = movementPreview.reachableTiles();
        for (GridPosition tile : tiles) {
            int distance = movementPreview.distanceTo(tile);
            boolean origin = tile.equals(selected.position());
            float intensity = origin ? 1f : 1f - (distance / (float) movementPreview.maxDistance());
            float fillR = origin ? 0.6f : 0.25f;
            float fillG = origin ? 0.85f : 0.85f;
            float fillB = origin ? 1f : 0.45f;
            float fillA = origin ? 0.4f : 0.22f + 0.12f * intensity;

            float outlineR = origin ? 0.9f : 0.35f;
            float outlineG = origin ? 0.95f : 0.95f;
            float outlineB = origin ? 1f : 0.45f;
            float outlineA = origin ? 0.95f : 0.7f;

            IsoTileHighlightRenderer.draw(
                level,
                tile.col(),
                tile.row(),
                fillR,
                fillG,
                fillB,
                fillA,
                outlineR,
                outlineG,
                outlineB,
                outlineA
            );
        }
    }

    private boolean allUnitsMoved(Squad squad) {
        if (squad == null) {
            return false;
        }
        for (Unit unit : squad.units()) {
            if (unit.isPlaced() && !unit.hasMovedThisTurn()) {
                return false;
            }
        }
        return true;
    }

    private void resetMovementFlags(Squad squad) {
        if (squad == null) {
            return;
        }
        for (Unit unit : squad.units()) {
            unit.setMovedThisTurn(false);
        }
    }

    private void executeEnemyTurn() {
        if (enemySquad == null || level == null) {
            return;
        }
        for (Unit enemy : enemySquad.units()) {
            if (enemy == null || !enemy.isPlaced()) {
                continue;
            }
            MovementPreview preview = MovementPreview.calculate(
                level,
                enemy,
                enemySquad.units(),
                playerSquad != null ? playerSquad.units() : List.of(),
                MOVEMENT_RANGE
            );
            if (preview == null) {
                continue;
            }
            List<GridPosition> tiles = preview.reachableTiles();
            tiles.removeIf(tile -> tile.equals(enemy.position()));
            if (tiles.isEmpty()) {
                continue;
            }
            GridPosition target = tiles.get(ThreadLocalRandom.current().nextInt(tiles.size()));
            enemy.placeAt(target);
        }
        contextualMessage = "Enemy units repositioned.";
        resetMovementFlags(enemySquad);
    }

    private void initializeBattleState() {
        battlePhase = BattlePhase.PLACEMENT;
        selectedUnitIndex = -1;
        movementPreview = null;
        loadoutTarget = null;
        partyReviewed = false;
        loadoutChosen = false;
        assignedLoadouts.clear();
        lastSelectedUnit = null;
        playerSquad = Squad.create(UnitFaction.PLAYER, PLAYER_SQUAD_SIZE);
        enemySquad = Squad.create(UnitFaction.ENEMY, ENEMY_SQUAD_SIZE);
        placementBannerVisible = true;
        placementController = new PlacementController(cursor, playerSquad, level.playerSpawnTiles());
        placeEnemySquad();
        List<GridPosition> spawnTiles = level.playerSpawnTiles();
        GridPosition focus = spawnTiles.isEmpty() ? level.startPosition() : spawnTiles.get(0);
        cursor.moveTo(focus);
        contextualMessage = "Press Enter to acknowledge the briefing, then deploy your squad.";
    }

    private void placeEnemySquad() {
        List<GridPosition> spawnTiles = level.enemySpawnTiles();
        if (spawnTiles.isEmpty()) {
            spawnTiles = List.of(level.exitPosition());
        }
        int size = spawnTiles.size();
        for (int i = 0; i < enemySquad.units().size(); i++) {
            GridPosition tile = spawnTiles.get(size == 0 ? 0 : i % size);
            enemySquad.units().get(i).placeAt(tile);
        }
    }

    private void enterActivePhase() {
        battlePhase = BattlePhase.ACTIVE;
        placementBannerVisible = false;
        contextualMessage = "All units deployed! Use Q and E to choose who moves next.";
        selectUnit(0);
    }

    private void drawText(float x, float y, String text, float r, float g, float b) {
        drawText(x, y, text, r, g, b, 1f);
    }

    private void drawText(float x, float y, String text, float r, float g, float b, float scale) {
        if (text == null || text.isEmpty()) {
            return;
        }
        textBuffer.clear();
        int quads = STBEasyFont.stb_easy_font_print(0f, 0f, text, null, textBuffer);
        glPushMatrix();
        glTranslatef(x, y, 0f);
        glScalef(scale, scale, 1f);
        glColor3f(r, g, b);
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(2, GL_FLOAT, 16, textBuffer);
        glDrawArrays(GL_QUADS, 0, quads * 4);
        glDisableClientState(GL_VERTEX_ARRAY);
        glPopMatrix();
    }

    private void cleanup() {
        if (window != NULL) {
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    private enum UiMode {
        CONFIG,
        PLAYING,
        PARTY_MENU,
        WEAPON_MENU
    }

    private enum BattlePhase {
        PLACEMENT,
        ACTIVE
    }
}
