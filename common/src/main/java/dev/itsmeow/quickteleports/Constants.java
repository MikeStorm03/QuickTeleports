package dev.itsmeow.quickteleports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Constants {
    static String MOD_ID = "quickteleports";
    static String CONFIG_FIELD_NAME = "teleport_request_timeout";
    static String CONFIG_FIELD_COMMENT = "Timeout until a teleport request expires, in seconds.";
    static int CONFIG_FIELD_VALUE = 30;
    static int CONFIG_FIELD_MIN = 0;
    static int CONFIG_FIELD_MAX = Integer.MAX_VALUE;

	static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
}
