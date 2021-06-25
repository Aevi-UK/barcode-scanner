# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.2.1] - 2021-06-25
* Updated build scripts and dependencies

## [2.2.0] - 2020-05-05
* Improved recognition when lighting is uneven and some parts of the QR code are darker than others

## [2.1.0] - 2020-02-20
* Bug fix about preview rotation not being computed correctly
* Build for all supported Android ABI
* Changed the camera exposure to have brighter previews
* Updated the API for better control over preview rendering

## [2.0.1] - 2019-05-24
* Bug fix about camera preview being upside down on some devices
* Bug fix about camera preview not starting under some circumstances

## [2.0.0] - 2018-12-20
* Library interface is now based on Java RX for easier multi-threading and increased flexibility
* Bug fix about the preview being sometimes upside down if quickly rotated from 0 to 180 degrees on some devices
* Added unit tests

## [1.1.0] - 2018-02-19
* Fixed black screen rotation issues
* Limited the framerate at which QRcodes are scanned

## [1.0.0] - 2017-11-02
* Library to easily create camera previews and scan QR codes