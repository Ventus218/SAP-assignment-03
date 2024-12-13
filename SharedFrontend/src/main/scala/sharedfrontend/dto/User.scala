package sharedfrontend.dto;

import upickle.default.*

case class User(username: Username, credit: Credit) derives ReadWriter
