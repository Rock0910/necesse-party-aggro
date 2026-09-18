package partyaggro.ui;

import java.awt.Rectangle;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormDropdownSelectionButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.GameInterfaceStyle;
import partyaggro.L;
import partyaggro.PartyAggroMod;
import partyaggro.data.AggroPlayerRef;
import partyaggro.data.AggroServerSection;
import partyaggro.data.AggroSource;
import partyaggro.util.ClientContext;

/** Standalone, draggable window for managing the hatred and whitelist. */
public class AggroManageForm extends Form {
    private static final int ROW_HEIGHT = 24;
    private static final int PAGE_SIZE = 8;
    private static final int COL_NAME = 4;
    private static final int COL_STATE = 164;
    private static final int COL_FIRST = 278;
    private static final int COL_LAST = 402;

    private final FormTextInput search;
    private final FormDropdownSelectionButton<String> filterDropdown;
    private final FormContentBox listBox;
    private final FormLabel countLabel;
    private final FormLabel pageLabel;
    private final FormTextButton prevButton;
    private final FormTextButton nextButton;
    private String lastFilter = "";
    private int lastVersion = -1;
    private int currentPage = 0;
    private int pageCount = 1;
    private boolean pendingRebuild = true;

    public AggroManageForm() {
        super("partyaggro_manage", 620, 364);
        GameInterfaceStyle ui = Settings.UI;
        int width = getWidth();

        this.addComponent(new FormLabel(L.t("manage"), new FontOptions(20), FormLabel.ALIGN_LEFT, 6, 6, width - 12));

        this.search = new FormTextInput(6, 34, FormInputSize.SIZE_32, width - 146, 64);
        this.search.placeHolder = new LocalMessage("partyaggro", "search_placeholder");
        this.addComponent(this.search);

        FormContentIconButton refresh = new FormContentIconButton(width - 140, 34, FormInputSize.SIZE_32,
                ButtonColor.BASE, ui.button_search_24, L.m("refresh"));
        refresh.onClicked(e -> PartyAggroMod.requestPlayers());
        this.addComponent(refresh);

        FormContentIconButton close = new FormContentIconButton(width - 96, 34, FormInputSize.SIZE_32,
                ButtonColor.BASE, ui.button_cross, L.m("close"));
        close.onClicked(e -> PartyAggroUi.closeManage());
        this.addComponent(close);

        FormContentIconButton clearAll = new FormContentIconButton(width - 52, 34, FormInputSize.SIZE_32,
                ButtonColor.BASE, ui.button_trash_24, L.m("clear_all"));
        clearAll.onClicked(e -> {
            AggroServerSection section = PartyAggroMod.CONFIG.get(ClientContext.currentWorldId());
            section.clearAllHatred();
            PartyAggroMod.save();
            PartyAggroMod.markDirty();
            this.pendingRebuild = true;
        });
        this.addComponent(clearAll);

        this.filterDropdown = new FormDropdownSelectionButton<String>(6, 72, FormInputSize.SIZE_32,
                ButtonColor.BASE, 190, L.m("filter"));
        this.filterDropdown.options.add("all", L.m("filter_all"));
        this.filterDropdown.options.add("hatred", L.m("filter_hatred"));
        this.filterDropdown.options.add("whitelist", L.m("filter_whitelist"));
        this.filterDropdown.options.add("none", L.m("filter_none"));
        this.filterDropdown.setSelected("all", L.m("filter_all"));
        this.filterDropdown.onSelected(e -> {
            this.currentPage = 0;
            this.pendingRebuild = true;
        });
        this.addComponent(this.filterDropdown);

        // Pagination: page number on the left, then the two buttons together.
        this.pageLabel = new FormLabel("", new FontOptions(14), FormLabel.ALIGN_RIGHT, width - 252, 78, 132);
        this.addComponent(this.pageLabel);

        this.prevButton = new FormTextButton("<", width - 116, 72, 34, FormInputSize.SIZE_32, ButtonColor.BASE);
        this.prevButton.onClicked(e -> {
            if (this.pageCount > 1) {
                this.currentPage = (this.currentPage - 1 + this.pageCount) % this.pageCount;
                this.pendingRebuild = true;
            }
        });
        this.addComponent(this.prevButton);

        this.nextButton = new FormTextButton(">", width - 78, 72, 34, FormInputSize.SIZE_32, ButtonColor.BASE);
        this.nextButton.onClicked(e -> {
            if (this.pageCount > 1) {
                this.currentPage = (this.currentPage + 1) % this.pageCount;
                this.pendingRebuild = true;
            }
        });
        this.addComponent(this.nextButton);

        // Column headers
        this.addComponent(new FormLabel(L.t("col_player"), new FontOptions(12), FormLabel.ALIGN_LEFT, COL_NAME, 112, COL_STATE - COL_NAME - 4));
        this.addComponent(new FormLabel(L.t("col_state"), new FontOptions(12), FormLabel.ALIGN_LEFT, COL_STATE, 112, COL_FIRST - COL_STATE - 4));
        this.addComponent(new FormLabel(L.t("col_first"), new FontOptions(12), FormLabel.ALIGN_LEFT, COL_FIRST, 112, COL_LAST - COL_FIRST - 4));
        this.addComponent(new FormLabel(L.t("col_last"), new FontOptions(12), FormLabel.ALIGN_LEFT, COL_LAST, 112, width - COL_LAST - 80));

        this.listBox = new FormContentBox(4, 130, width - 8, 200);
        this.addComponent(this.listBox);

        this.countLabel = new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, 6, 338, width - 12);
        this.addComponent(this.countLabel);

        try {
            setDraggingBox(new Rectangle(0, 0, getWidth(), 30));
        } catch (Throwable ignored) {
        }

        this.rebuild();
    }

    @Override
    public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
        try {
            tryPutOnTop();
        } catch (Throwable ignored) {
        }
        super.handleInputEvent(event, tickManager, perspective);
    }

    /** Called every frame while visible. */
    public void tick() {
        String filter = this.search.getText() == null ? "" : this.search.getText();
        if (!this.lastFilter.equals(filter)) {
            this.lastFilter = filter;
            this.currentPage = 0;
            this.pendingRebuild = true;
        }
        if (this.lastVersion != AggroManageCache.version) {
            this.lastVersion = AggroManageCache.version;
            this.pendingRebuild = true;
        }
        if (this.pendingRebuild) {
            this.pendingRebuild = false;
            this.rebuild();
        }
    }

    private void rebuild() {
        this.listBox.clearComponents();
        final long worldId = ClientContext.currentWorldId();
        final AggroServerSection section = PartyAggroMod.CONFIG.get(worldId);
        GameInterfaceStyle ui = Settings.UI;
        String nameFilter = this.search.getText() == null ? "" : this.search.getText().toLowerCase();
        String stateFilter = "all";
        try {
            stateFilter = this.filterDropdown.getSelected();
        } catch (Throwable ignored) {
        }
        if (stateFilter == null) {
            stateFilter = "all";
        }
        long self = ClientContext.selfAuth();

        // Filter
        List<AggroPlayerRef> filtered = new ArrayList<AggroPlayerRef>();
        for (AggroPlayerRef player : AggroManageCache.players) {
            if (!nameFilter.isEmpty() && (player.name == null || !player.name.toLowerCase().contains(nameFilter))) {
                continue;
            }
            boolean hated = section.isHatred(player.authentication);
            boolean white = section.isWhitelist(player.authentication);
            boolean pass;
            if ("hatred".equals(stateFilter)) {
                pass = hated;
            } else if ("whitelist".equals(stateFilter)) {
                pass = white;
            } else if ("none".equals(stateFilter)) {
                pass = !hated && !white;
            } else {
                pass = true;
            }
            if (pass) {
                filtered.add(player);
            }
        }

        // Sort: most recently online first, then by name.
        Collections.sort(filtered, new Comparator<AggroPlayerRef>() {
            @Override
            public int compare(AggroPlayerRef a, AggroPlayerRef b) {
                int byTime = Long.compare(b.lastOnline, a.lastOnline);
                return byTime != 0 ? byTime : a.name.compareToIgnoreCase(b.name);
            }
        });

        // Paginate
        int pages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (this.currentPage >= pages) {
            this.currentPage = pages - 1;
        }
        if (this.currentPage < 0) {
            this.currentPage = 0;
        }
        int from = this.currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filtered.size());

        int listWidth = getWidth() - 8;
        int rowY = 0;
        for (int i = from; i < to; i++) {
            final AggroPlayerRef player = filtered.get(i);
            boolean isSelf = self >= 0L && player.authentication == self;
            boolean hated = section.isHatred(player.authentication);
            boolean white = section.isWhitelist(player.authentication);
            String state = isSelf ? L.t("state_self")
                    : (hated ? L.t("state_hatred") : (white ? L.t("state_whitelist") : L.t("state_none")));
            if (hated) {
                AggroSource source = section.sourceOf(player.authentication);
                state = state + (source == null ? "" : " (" + L.t(source == AggroSource.AUTO ? "src_auto" : "src_manual") + ")");
            }

            this.listBox.addComponent(new FormLabel(player.name, new FontOptions(12),
                    FormLabel.ALIGN_LEFT, COL_NAME, rowY + 4, COL_STATE - COL_NAME - 4));
            this.listBox.addComponent(new FormLabel(state, new FontOptions(12),
                    FormLabel.ALIGN_LEFT, COL_STATE, rowY + 4, COL_FIRST - COL_STATE - 4));
            this.listBox.addComponent(new FormLabel(formatTime(player.firstSeen), new FontOptions(12),
                    FormLabel.ALIGN_LEFT, COL_FIRST, rowY + 4, COL_LAST - COL_FIRST - 4));
            this.listBox.addComponent(new FormLabel(formatTime(player.lastOnline), new FontOptions(12),
                    FormLabel.ALIGN_LEFT, COL_LAST, rowY + 4, listWidth - COL_LAST - 76));

            int buttonY = rowY + 1;
            if (!isSelf) {
                FormContentIconButton hateButton = new FormContentIconButton(listWidth - 72, buttonY,
                        FormInputSize.SIZE_20, ButtonColor.BASE, ui.priority_top, L.m("set_hatred"));
                hateButton.onClicked(e -> {
                    section.addHatred(player.authentication, AggroSource.MANUAL);
                    PartyAggroMod.save();
                    PartyAggroMod.markDirty();
                    this.pendingRebuild = true;
                });
                this.listBox.addComponent(hateButton);
            }

            FormContentIconButton whiteButton = new FormContentIconButton(listWidth - 48, buttonY,
                    FormInputSize.SIZE_20, ButtonColor.BASE, ui.button_checked_20, L.m("set_whitelist"));
            whiteButton.onClicked(e -> {
                section.addWhitelist(player.authentication);
                PartyAggroMod.save();
                PartyAggroMod.markDirty();
                this.pendingRebuild = true;
            });
            this.listBox.addComponent(whiteButton);

            FormContentIconButton clearButton = new FormContentIconButton(listWidth - 24, buttonY,
                    FormInputSize.SIZE_20, ButtonColor.BASE, ui.button_minus_20, L.m("set_clear"));
            clearButton.onClicked(e -> {
                section.clearPlayer(player.authentication);
                PartyAggroMod.save();
                PartyAggroMod.markDirty();
                this.pendingRebuild = true;
            });
            this.listBox.addComponent(clearButton);

            rowY += ROW_HEIGHT;
        }

        this.listBox.setContentBox(new Rectangle(0, 0, listWidth, Math.max(rowY, 1)));
        this.listBox.setHeight(Math.min(200, Math.max(ROW_HEIGHT, rowY)));
        this.pageLabel.setText(L.tf("page", "page", String.valueOf(this.currentPage + 1), "pages", String.valueOf(pages)));
        this.pageCount = pages;
        this.prevButton.setActive(pages > 1);
        this.nextButton.setActive(pages > 1);
        this.countLabel.setText(L.tf("showing", "shown", String.valueOf(filtered.size()),
                "total", String.valueOf(AggroManageCache.players.size())));
    }

    private static String formatTime(long epoch) {
        if (epoch <= 0L) {
            return L.t("na");
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(epoch));
        } catch (Throwable t) {
            return L.t("na");
        }
    }
}
