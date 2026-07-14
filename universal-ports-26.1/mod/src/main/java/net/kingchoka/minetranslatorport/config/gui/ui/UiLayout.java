package net.kingchoka.minetranslatorport.config.gui.ui;

public record UiLayout(
    int panelX, int panelY, int panelWidth, int panelHeight,
    int headerHeight, int footerHeight, int sidebarWidth,
    int contentX, int contentY, int contentWidth, int contentHeight,
    boolean compact
) {
    public static UiLayout calculate(int screenWidth, int screenHeight) {
        int margin = screenHeight <= 500 ? 8 : 16;
        int panelWidth = Math.min(960, Math.max(620, screenWidth - margin * 2));
        panelWidth = Math.min(panelWidth, screenWidth - 8);
        int panelHeight = Math.min(620, Math.max(440, screenHeight - margin * 2));
        panelHeight = Math.min(panelHeight, screenHeight - 8);
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = (screenHeight - panelHeight) / 2;
        boolean compact = panelWidth < 760 || panelHeight < 520;
        int headerHeight = compact ? 38 : 44;
        int footerHeight = compact ? 38 : 44;
        int sidebarWidth = compact ? 126 : 164;
        int contentX = panelX + sidebarWidth;
        int contentY = panelY + headerHeight;
        int contentWidth = panelWidth - sidebarWidth;
        int contentHeight = panelHeight - headerHeight - footerHeight;
        return new UiLayout(panelX, panelY, panelWidth, panelHeight, headerHeight, footerHeight,
            sidebarWidth, contentX, contentY, contentWidth, contentHeight, compact);
    }

    public int footerY() {
        return panelY + panelHeight - footerHeight;
    }
}
