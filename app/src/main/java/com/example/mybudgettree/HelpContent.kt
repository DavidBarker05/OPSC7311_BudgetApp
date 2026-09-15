package com.example.mybudgettree

enum class HelpTopic { GENERAL, ACCOUNT, SERVICES }

enum class ContactKind { EMAIL, WEBSITE, FACEBOOK, WHATSAPP, INSTAGRAM }

data class FaqEntry(
    val id: String,
    val topic: HelpTopic,
    val titleRes: Int,
    val bodyRes: Int
)

data class ContactEntry(
    val kind: ContactKind,
    val titleRes: Int,
    val iconRes: Int
)

object HelpContent {
    val faqs = listOf(
        FaqEntry("use", HelpTopic.GENERAL, R.string.faq_use_title, R.string.faq_use_body),
        FaqEntry("cost", HelpTopic.GENERAL, R.string.faq_cost_title, R.string.faq_cost_body),
        FaqEntry("contact", HelpTopic.SERVICES, R.string.faq_contact_title, R.string.faq_contact_body),
        FaqEntry("password", HelpTopic.ACCOUNT, R.string.faq_password_title, R.string.faq_password_body),
        FaqEntry("privacy", HelpTopic.GENERAL, R.string.faq_privacy_title, R.string.faq_privacy_body),
        FaqEntry("settings", HelpTopic.ACCOUNT, R.string.faq_settings_title, R.string.faq_settings_body),
        FaqEntry("delete", HelpTopic.ACCOUNT, R.string.faq_delete_title, R.string.faq_delete_body),
        FaqEntry("history", HelpTopic.SERVICES, R.string.faq_history_title, R.string.faq_history_body),
        FaqEntry("offline", HelpTopic.GENERAL, R.string.faq_offline_title, R.string.faq_offline_body)
    )

    val contacts = listOf(
        ContactEntry(ContactKind.EMAIL, R.string.customer_service, R.drawable.ic_help_headset),
        ContactEntry(ContactKind.WEBSITE, R.string.website, R.drawable.ic_help_web),
        ContactEntry(ContactKind.FACEBOOK, R.string.facebook, R.drawable.ic_help_facebook),
        ContactEntry(ContactKind.WHATSAPP, R.string.whatsapp, R.drawable.ic_help_whatsapp),
        ContactEntry(ContactKind.INSTAGRAM, R.string.instagram, R.drawable.ic_help_instagram)
    )
}

sealed class HelpListItem {
    data class Faq(val entry: FaqEntry, val expanded: Boolean) : HelpListItem()
    data class Contact(val entry: ContactEntry) : HelpListItem()
}
