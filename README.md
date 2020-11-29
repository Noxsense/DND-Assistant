# DND App

This lil' D&D app is a little assistant
that will be handy to help you during your spontaneous D&D Sessions
while you travel in a train,
or in any other situation.

The goal of this app is to have all character stats
and some small dice fast by your hand.
It should also help you to maintain all your spells
and even the stuff in your character's inventory.

Even the character's history and their new adventures can be listed here.

## Platforms

As mentioned, the main goal of this app is to be handy and by your side.
The best option therefore is on your phone.

But also a good command line tool, since we love that.
Also a big screen desktop interface can be quite comfortable to use,
so it's also a good platform.

## This project's structure and How to get the Programms

The project is written in Kotlin and focussed to stay with that.

For configs and settings (eg. the save files), obviously not.

The source code is saved in the directory structure `./sourceCode`

### How to compile

Enter the directory `./sourceCode`.

There is the `gradle` project, you can simply run with `./gradlew build`.

For the desktop app run `./gradlew cui:run`.

If you just want the latest `debug.apk`, run `./gradlew assemlbleDebug` or `./gradlew installDebug`.

### Work in Progress

The app is not done yet,
there is even here and there some huge refactoring in progress.
Most likely, the most recent commits can be found in [development](https://github.com/Noxsense/DND-Assistent/tree/development).

Beside having a nice tool to play with, another goal of this project is,
to learn more about `Kotlin` and get comfortable with Android Development.
