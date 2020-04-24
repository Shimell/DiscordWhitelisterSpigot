# Discord Whitelister Bot for Spigot

A simple spigot plugin which allows whitelisting through a discord text channel. This provides an easy way for users to whitelist without needing to be on the minecraft server.

### Backwards compatibility

Version 1.2.0 and greater are not compatible with version 1.1.x and lower.
The layout of the internal user-list.yml got updated.
You need to remove it manually and let the plugin on v1.2.x create a new one.
If you upgrade without knowing what you are doing. Registration will not work correctly.

### Features:

- 3 separate role groups:
	- **add-remove group**: allows the user to add and remove users from the whitelist an unlimited amount of times (recommended for owners and admins)
	- **add group**: allows the user to add to the whitelist an unlimited amount of times (recommended for moderators)
	- **limited-add group**: allows the user to whitelist a limited amount of times (recommended for users, default amount is 3)
	- limited-add group can be disabled in the config (enabled by default)
	
- Removed list:
	- This list removes the ability for limited-add users to add back users that have been removed by the add-remove group
	- Can be disabled in the config (enabled by default)

- Discord commands:
    - Use `!whitelist add "minecraftUsername"` in a valid channel to whitelist people on your minecraft server
    - Use `!whitelist remove "minecraftUsername"` in a valid channel to remove people from the whitelist on your minecraft server
    - Use `!whitelist` in a valid channel to get info about the bot and how to use it

- Automatically add/remove a role when adding/removing to/from the whitelist
    - This feature is meant to be used when users can add themselves to the whitelist.
    - If `whitelisted-role-auto-add` is set to true (false by default), the Discord role with the name defined by `whitelisted-role` ("Whitelisted" by default) will be added to that user when they successfully add (themselves) to the whitelist.
    - If `whitelisted-role-auto-remove` is set to true (false by default), that role will be removed from that user when they successfully remove (themselves) from the whitelist.
    - This requires:
        - The bot to have the `Manage Roles` permission in Discord
        - Setting up a Discord role with the same name (case sensitive) as the config
        - The bot's role must be higher than the whitelist role 

- Only select Discord roles can whitelist through the bot
- Bot only listens for messages in select text channels
- Logs whitelist attempts from valid roles in the console

### Set Up:

Config file is located at: (server-root)/plugins/DiscordWhitelister - this needs a valid bot token and valid channel id(s) to work.
To create a Discord application and/or find your discord bot token, follow this link: https://discordapp.com/developers/applications/
