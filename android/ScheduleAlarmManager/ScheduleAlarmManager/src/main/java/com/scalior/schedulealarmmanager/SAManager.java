package com.scalior.schedulealarmmanager;

import android.content.Context;

/**
 * Created by eyong on 9/25/14.
 */
public class SAManager {
    private static SAManager m_instance;

    private Context m_context;


    public static SAManager getInstance(Context p_context) {
        if (m_instance == null ) {
            m_instance = new SAManager(p_context);
        }
        return m_instance;
    }

    private SAManager(Context p_context) {
        m_context = p_context;
    }
}
