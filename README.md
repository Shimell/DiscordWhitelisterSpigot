# Discord Whitelister Bot for Spigot

A simple spigot plugin which allows whitelisting through a discord text channel. This provides an easy way for users to whitelist without needing to be on the minecraft server.

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
    
- Custom Message Support:
    - Allows editing of server response messages (file is located at `(server-root)/plugins/DiscordWhitelister/custom-messages.yml)`
    - This feature is disabled by default and can be enabled in the config file (`use-custom-messages`)
    - Note: Only message variables ({Sender} for example) in the original messages will be evaluated. For example: using {MaxWhitelistAmount} in the "insufficient-permissions" string/message will not work as it was never in the original string/message.

- Use Discord server/guild role ids instead of role names:
	- Allows the use of ids of roles instead of names, for example: `445666895333687347` instead of `Admin`
	- To enable this set '`use-id-for-roles`' to `true`
	- Example of relevant fields changed in the config to use ids:
		``` yaml
		add-remove-roles:
		- 446223693887176704
		add-roles:
		- 485463455940214794
		limited-add-roles:
		- 639221397981233162
		use-id-for-roles: true
		```
		
- Reload command:
	- Use the command '`dwreload`' or '`discordwhitelisterreload`' to reload the config and re-initialize the bot without having to restart the Minecraft server

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

Config file is located at: `(server-root)/plugins/DiscordWhitelister/discord-whitelister.yml`, **this needs a valid bot token and valid channel id(s) to work**.
To create a Discord application and/or find your discord bot token, follow this link: https://discordapp.com/developers/applications/

Here is a short video showing all the steps needed to configure the bot: https://youtu.be/OqaeItuLefU

### Backwards compatibility (Only applies to versions lower than v1.2.0)

Version 1.2.0 onwards are not compatible with version 1.1.x and lower. This is due to the layout of user-list.yml being changed. You will need to remove user-list.yml manually and let the plugin create a new one.
If you upgrade without doing so, registration will not work correctly.
