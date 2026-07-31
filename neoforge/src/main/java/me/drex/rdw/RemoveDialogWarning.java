package me.drex.rdw;

import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;

public interface RemoveDialogWarning {
    
    public static final String MOD_ID = "remove-dialog-warning";

    public static final String COMMAND_KEY = MOD_ID + ":command";
    public static final String DYNAMIC_KEY = MOD_ID + ":dynamic";
    public static final String BOOLEAN_TAGS_KEY = MOD_ID + ":boolean_input";
    public static final String STRING_INPUT_KEY = MOD_ID + ":string_input";
    public static final Identifier DIALOG_ACTION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "run_command");
    public static final ScopedValue<Dialog> DIALOG_SCOPE = ScopedValue.newInstance();
}
