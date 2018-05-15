package com.anair.demo.component.movieratingsplitter.util;

import com.google.common.base.Joiner;
import org.apache.commons.collections4.MapUtils;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class SplitterHelper {

    private static final String EMPTY = "";

    private SplitterHelper() {
    }

    public static boolean isNotEmpty(final Object object) {
        return !isEmpty(object);
    }

    public static boolean isEmpty(final Object object) {
        return ObjectUtils.isEmpty(object);
    }

    public static String buildURIParamsUsing(Properties configs) {
        if(isEmpty(configs) || MapUtils.isEmpty(new HashMap<>(configs))) {
            return EMPTY;
        }
        Map<?, ?> configsMap = new HashMap<>(configs);
        return Joiner.on("&").withKeyValueSeparator("=").join(configsMap);
    }
}
