AndroidBluetoothProtobuf
========================

A library for bluetooth communication between Android and PC running Java and based on Google Protobuf messages.

With this library, it is easier to establish bluetooth connections between a Java App on a PC and an Android Device.
Only one class is needed to start listing for new connections and creating new connections to send messages.
The messages are send via Input/Output-streams. To support arbitrary simple or complicated messages, Google protobuf is used
to encapsulate the data. The library will also take care of the type of message and message length.


Requirements
============
Following libraries are used on both sides:
 * log4j 1.2.17 (android-logging-log4j for Android)
 * protobuf 2.4.1
 
For the PC side additionally:
 * bluecove 2.1.1 SNAPSHOT
 * bluecove-gpl or preferrably bluecove-bluez (supports dbus, no device inquiry needed and first connection)


Usage
=====
There are three jars:
 * btpb_core.jar: Needed for Android and Java
 * btpb_android.jar: Only Android specific libs
 * btpb_java.jar: Only Java specific libs

Also, you need to define your Messages. This can simply be done with a custom Enum with all your protobuf messages. An example can be found in *BluetoothProtobufMessages*. It is a good idea to have the Enum and the Protobuf files in an own project and package it into a jar file, because it is needed by both sides and should be identical.

Additionally you need to add the jars mentioned under Requirements to your build path. You can find them under 
BluetoothProtobufCore/lib/

See the small sample applications under *BluetoothProtobufClient* and *BluetoothProtobufApp* for how to use the library.

Compile
=======
The Source is organized in Eclipse projects. It is not possible to put everything into one jar file, because the Android Java compiler complains about native libs from bluecove. Also, I wanted to separate the lib from the examples.
There are some project dependencies, so you should import all projects.
The library can be build with the jardesc files in the Core project.
