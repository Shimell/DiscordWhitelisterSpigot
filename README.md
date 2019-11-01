# Discord Whitelister Bot for Spigot

A simple spigot plugin which allows whitelisting through a discord text channel. This provides an easy way for users to whitelist without needing to be on the minecraft server.

##### Features:

- 3 separate role groups:
	- add-remove group: allows the user to add and remove users from the whitelist an unlimited amount of times (recommended for owners and admins)
	- add group: allows the user to add to the whitelist an unlimited amount of times (recommended for moderators)
	- limited-add group: allows the user to whitelist a limited amount of times (recommended for users, default amount is 3)
	- limited-add group can be disabled in the config (enabled by default)
	
- removed list:
	- this list removes the ability for limited-add users to add back users that have been removed by the add-remove group
	- can be disabled in the config (enabled by default)

- use '!whitelist add "minecraftUsername"' in a valid channel to whitelist people on your minecraft server
- use '!whitelist remove "minecraftUsername"' in a valid channel to remove people from the whitelist on your minecraft server
- use '!whitelist' in a valid channel to get info about the bot and how to use it

- only select Discord roles can whitelist through the bot
- bot only listens for messages in select text channels
- logs whitelist attempts from valid roles in the console

##### Set Up:

Config file is located at: (server-root)/plugins/DiscordWhitelister - this needs a valid bot token and valid channel ids to work.
To create a Discord application and/or find your discord bot token, follow this link: https://discordapp.com/developers/applications/
