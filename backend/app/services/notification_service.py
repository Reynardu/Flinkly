async def notify_household_members(members: list, title: str, body: str, exclude_user_id: int | None = None, data: dict | None = None):
    # Push-Benachrichtigungen werden über WebSocket-Events übermittelt.
    # Für native Android Push-Notifications kann hier später FCM ergänzt werden.
    pass
