package net.kingchoka.minetranslator.config.gui.ui.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Sidebar {
    private final List<SidebarButton> buttons = new ArrayList<>();
    public void add(SidebarButton button){buttons.add(button);}
    public List<SidebarButton> buttons(){return Collections.unmodifiableList(buttons);}
}
