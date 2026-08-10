signingConfigs {
        create("release") {
            // CHANGE THIS: Change 'my-upload-keybeesharp.jks' to match your new filename
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-keywc.jks"
            storeFile = file(keystorePath)
            
            storePassword = System.getenv("STORE_PASSWORD") ?: project.findProperty("STORE_PASSWORD")?.toString()
            keyAlias = "upload"
            keyPassword = System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD")?.toString()
        }
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isCrunchPngs = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
