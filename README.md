# DiscordWhitelisterBot-Spigot

A simple spigot plugin which allows whitelisting through a discord text channel.
This provides an easy way for staff members on a server to whitelist outside the server whilst not having full access to the console.

Features:
- use '!whitelist add <MinecraftUsername>' in a valid channel to whitelist people on your minecraft server
- use '!whitelist remove <MinecraftUsername>' in a valid channel to remove people from the whitelist on your minecraft server
- use '!whitelist' in a valid channel to get info about the bot and how to use it
- only select Discord roles can whitelist through the bot
- bot only listens for messages in select text channels
- logs whitelist attempts from valid roles in the console

Config file is located at: (server-root)/plugins/DiscordWhitelister - this needs a valid bot token and valid channel ids to work.
To create a Discord application and/or find your discord bot token, follow this link: https://discordapp.com/developers/applications/
