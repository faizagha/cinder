rootProject.name = "cinder"

include("backend:note-service")
project(":backend:note-service").projectDir = file("backend/note-service")
