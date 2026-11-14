package com.example.core

object AppConfig {
    const val APP_NAME_EN = "Mobile Code Studio"
    const val APP_NAME_FA = "موبایل کد استودیو"
    const val APP_VERSION = "1.0.0"
    const val BUILD_NUMBER = 1
    const val DEVELOPER_NAME = "R.I.T.M"
    const val COPYRIGHT_LINE = "© 2026 R.I.T.M. All Rights Reserved."
    const val COPYRIGHT_NOTICE_EN =
        "All rights reserved. Unauthorized redistribution, modification, or commercial reproduction of this application is prohibited except as permitted by applicable law and the licenses of included third-party components."
    const val COPYRIGHT_NOTICE_FA =
        "تمامی حقوق محفوظ است. توزیع مجدد، تغییر یا تکثیر تجاری غیرمجاز این برنامه ممنوع است، مگر در مواردی که قانون مجاز بداند یا تحت مجوزهای اجزای متن‌باز همراه برنامه مجاز شده باشد."

    const val DEFAULT_SERVER_PORT = 8080
    const val MIN_SERVER_PORT = 1024
    const val MAX_SERVER_PORT = 65535

    const val DEFAULT_TOOLBAR_SYMBOLS = "{ } [ ] ( ) < > ; : = \" ' / ! & # ."

    val SUPPORTED_EXTENSIONS = listOf(
        "html", "htm", "css", "js", "json", "txt", "xml", "svg", "md"
    )
}
