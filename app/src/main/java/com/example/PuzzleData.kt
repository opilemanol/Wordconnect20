package com.example

object PuzzleData {
    val puzzlePool = listOf(
        Pair("DROW", listOf("WORD", "ROW", "ROD")),
        Pair("ACTS", listOf("CATS", "CAT", "SAT", "ACT")),
        Pair("PLAY", listOf("PLAY", "LAY", "PAY", "LAP")),
        Pair("ARTS", listOf("STAR", "ART", "RAT", "SAT", "TAR")),
        Pair("LION", listOf("LION", "OIL", "NIL", "ION")),
        Pair("BOTA", listOf("BOAT", "BAT", "BOA", "TAB")),
        Pair("NOSE", listOf("NOSE", "SON", "ONE", "EON")),
        Pair("CLOD", listOf("COLD", "OLD", "COD", "DOC")),
        Pair("YARD", listOf("YARD", "RAY", "DAY", "DRY")),
        Pair("MONO", listOf("MOON", "NO", "ON", "MONO")),
        Pair("GOLD", listOf("GOLD", "GOD", "LOG", "OLD")),
        Pair("REFIE", listOf("FIRE", "FREE", "RIFE", "REF")),
        Pair("STOP", listOf("STOP", "POST", "POTS", "TOP", "POT")),
        Pair("EAST", listOf("EAST", "SEAT", "TEA", "EAT", "ATE")),
        Pair("DEER", listOf("DEER", "RED", "REED")),
        Pair("LINE", listOf("LINE", "NIL", "LIE")),
        Pair("WIND", listOf("WIND", "WIN", "DIN")),
        Pair("GAME", listOf("GAME", "MAGE", "GEM", "AGE")),
        Pair("LIFE", listOf("LIFE", "FILE", "ELF", "LIE")),
        Pair("WOLF", listOf("FLOW", "WOLF", "LOW", "OWL")),
        Pair("KABE", listOf("BAKE", "BEAK")), // Fixed: "CAB" cannot be spelled (no C)
        Pair("NEST", listOf("NEST", "NET", "TEN", "SENT")),
        Pair("PARK", listOf("PARK", "RAP", "ARK")),
        Pair("LUBE", listOf("BLUE", "LUBE", "BEL")),
        Pair("CLOA", listOf("COAL", "COLA", "LAC")),
        Pair("SUTR", listOf("RUST", "RUT", "RUTS")),
        Pair("SAPE", listOf("APES", "PEAS", "SPA", "ASP")),
        Pair("TEAM", listOf("TEAM", "MATE", "MEAT", "TAME", "TEA")),
        Pair("LAMP", listOf("LAMP", "PAL", "LAP", "MAP")),
        Pair("SINK", listOf("SINK", "INK", "KIN", "SKI")),
        Pair("COAT", listOf("COAT", "TACO", "CAT", "COT", "ACT")),
        Pair("ROSE", listOf("ROSE", "SORE", "ORE")),
        Pair("DRECK", listOf("DECK", "KED", "REC")),
        Pair("SANG", listOf("SANG", "NAG", "GAS", "SAG")),
        Pair("CORN", listOf("CORN", "CON", "NOR")),
        Pair("FROG", listOf("FROG", "FOR", "FOG")),
        Pair("RIDE", listOf("RIDE", "RED", "RID", "DIE")),
        Pair("FSIH", listOf("FISH", "HIS", "IFS")),
        Pair("MILK", listOf("MILK", "ILK", "MIL")),
        Pair("CARE", listOf("CARE", "RACE", "CAR", "ARC")),
        Pair("NOTE", listOf("NOTE", "TONE", "ONE", "TEN")),
        Pair("MARE", listOf("MARE", "REAM", "ARM", "EAR")),
        Pair("SALT", listOf("LAST", "SALT", "SAT", "ALT")),
        Pair("PEAR", listOf("PEAR", "REAP", "APE", "EAR")),
        Pair("RATE", listOf("RATE", "TEAR", "EAR", "ATE")),
        Pair("DARE", listOf("DARE", "READ", "RED", "DEAR")),
        Pair("MEAT", listOf("MEAT", "TEAM", "MATE", "TEA")),
        Pair("RING", listOf("RING", "GRIN", "GIN", "RIG")),
        Pair("TILE", listOf("TILE", "LITE", "LIE", "TIE")),

        Pair("CARE", listOf("ACRE", "RACE", "CAR", "ARE")),
        Pair("BEND", listOf("BEND", "BED", "END", "DEN")),
        Pair("FARM", listOf("FARM", "ARM", "FAR", "RAM")),
        Pair("MIND", listOf("MIND", "DIM", "DIN", "MID")),
        Pair("BIRD", listOf("BIRD", "RIB", "RID", "BID")),
        Pair("COIN", listOf("COIN", "ICON", "CON", "ION")),
        Pair("FISH", listOf("FISH", "HIS", "IF")), // Fixed redundant / non-standard "ISH"
        Pair("ROAD", listOf("ROAD", "ROD", "OAR", "ADO")),
        Pair("WAKE", listOf("WAKE", "WEAK", "AWE")), // Changed CAKE clone to WAKE to avoid duplicate keys and "AKE" spelling issue
        Pair("MOON", listOf("MOON", "MOO", "NO")), // Removed nonstandard "ONO"

        Pair("HAND", listOf("HAND", "AND", "HAD", "DNA")),
        Pair("SING", listOf("SING", "SIGN", "GIN", "SIN")),
        Pair("WARM", listOf("WARM", "ARM", "WAR", "RAM")),
        Pair("CAMP", listOf("CAMP", "MAP", "CAP", "AMP")),
        Pair("BELL", listOf("BELL", "ELL", "BEL")),
        Pair("KING", listOf("KING", "INK", "GIN", "KIN")),
        Pair("CORN", listOf("CORN", "CON", "NOR", "ROC")),
        Pair("FORK", listOf("FORK", "FOR")), // Simplified
        Pair("MILK", listOf("MILK", "ILK", "MIL")), // "KIM" is a name
        Pair("BOOK", listOf("BOOK", "BOO")), // "KOO" is nonstandard

        Pair("SNOW", listOf("SNOW", "NOW", "OWN", "SON")),
        Pair("TREE", listOf("TREE", "ERE", "TEE")),
        Pair("DUST", listOf("DUST", "STUD")), // Removed nonstandard/slang "DUS", "TUS"
        Pair("FLAG", listOf("FLAG", "LAG", "FAG", "GAL")),
        Pair("SHIP", listOf("SHIP", "HIP", "HIS", "SIP")),
        Pair("BRICK", listOf("BRICK", "RIB", "ICK")), // Removed double "BRICK" entry
        Pair("CHAIR", listOf("CHAIR", "HAIR", "AIR", "ARC")),
        Pair("PLANT", listOf("PLANT", "PLAN", "ANT", "TAN")),
        Pair("STONE", listOf("STONE", "NOTE", "TON", "ONE")),
        Pair("HOUSE", listOf("HOUSE", "USE", "SHE", "HOE")),

        Pair("APPLE", listOf("APPLE", "PAL", "PEA", "LAP")),
        Pair("MOUSE", listOf("MOUSE", "SUM", "USE")), // "MOE" is nonstandard / name
        Pair("TRAIN", listOf("TRAIN", "RAIN", "RAN", "ANT")),
        Pair("PLANE", listOf("PLANE", "PEN", "LAP", "LEAN")),
        Pair("CROWN", listOf("CROWN", "COW", "NOW", "OWN")),
        Pair("HEART", listOf("HEART", "HEAR", "RATE", "EAR")),
        Pair("LIGHT", listOf("LIGHT", "HIT", "LIT")), // Removed "GIT"
        Pair("SMILE", listOf("SMILE", "LIME", "MILE", "LIE")),
        Pair("BRAIN", listOf("BRAIN", "RAIN", "RAN", "AIR")),
        Pair("SWEET", listOf("SWEET", "WEST", "SEE", "TEE")),

        Pair("CLEAN", listOf("CLEAN", "LACE", "CAN", "LEAN")),
        Pair("SUGAR", listOf("SUGAR", "RAG", "RUG", "SAG")),
        Pair("WATER", listOf("WATER", "WEAR", "RATE", "TEAR")),
        Pair("POWER", listOf("POWER", "PORE", "ROW", "ORE")),
        Pair("GREEN", listOf("GREEN", "GENE", "ERG", "NEE")),
        Pair("BLACK", listOf("BLACK", "BACK", "LACK", "CAB")),
        Pair("WHITE", listOf("WHITE", "WITH", "HIT", "THE")),
        Pair("BROWN", listOf("BROWN", "OWN", "ROW", "NOW")),
        Pair("PINK", listOf("PINK", "INK", "PIN", "KIN")),
        Pair("ORANGE", listOf("ORANGE", "RAGE", "ONE", "RAN")),

        Pair("PURPLE", listOf("PURPLE", "PURE", "RULE", "PER")),
        Pair("YELLOW", listOf("YELLOW", "YELL", "LOW", "OWL")),
        Pair("MARKET", listOf("MARKET", "MAKE", "RATE", "TEAM", "TEAR")),
        Pair("POCKET", listOf("POCKET", "POKE", "TOP", "COP")),
        Pair("GARDEN", listOf("GARDEN", "DANGER", "RAG", "DEN")),
        Pair("BOTTLE", listOf("BOTTLE", "BOLT", "LOT")), // Removed "TEL"
        Pair("SCHOOL", listOf("SCHOOL", "COOL", "SOLO", "COL")),
        Pair("PENCIL", listOf("PENCIL", "CLIP", "PEN", "NIL")),
        Pair("WINDOW", listOf("WINDOW", "WIND", "OWN", "NOW")),
        Pair("MARKER", listOf("MARKER", "MAKE", "ARM", "EAR")),

        Pair("BUTTON", listOf("BUTTON", "TON", "NOT", "BUT")),
        Pair("STREAM", listOf("STREAM", "MASTER", "TEAM", "RATE")),
        Pair("FLOWER", listOf("FLOWER", "WOLF", "LOW", "ROW")),
        Pair("PLANET", listOf("PLANET", "PLANE", "LATE", "NET")),
        Pair("MONKEY", listOf("MONKEY", "KEY", "MEN", "ONE")),
        Pair("JACKET", listOf("JACKET", "JACK", "TAKE", "TEA")),
        Pair("BASKET", listOf("BASKET", "BAKE", "SEAT", "TAB")),
        Pair("FATHER", listOf("FATHER", "HEAR", "RATE", "FAR")),
        Pair("MOTHER", listOf("MOTHER", "HOME", "MORE", "THE")),
        Pair("SISTER", listOf("SISTER", "SITE", "REST", "TIRE")),

        Pair("BROTHER", listOf("BROTHER", "OTHER", "HER", "HOT")),
        Pair("FRIEND", listOf("FRIEND", "FIND", "RED", "END")),
        Pair("BREAD", listOf("BREAD", "BEAR", "READ", "RED")),
        Pair("BUTTER", listOf("BUTTER", "TUBE", "RUB", "BET")),
        Pair("CHEESE", listOf("CHEESE", "SEE", "SHE")), // Removed nonstandard "CHEE"
        Pair("MARKS", listOf("MARKS", "MASK", "ARK", "ARM")),
        Pair("CIRCLE", listOf("CIRCLE", "CLERIC", "ICE", "LIE")),
        Pair("SQUARE", listOf("SQUARE", "SURE", "USER", "SEA")),
        Pair("TRIANGLE", listOf("TRIANGLE", "ANGLE", "RING", "ANT")),
        Pair("COUNTRY", listOf("COUNTRY", "COUNT", "TON", "TRY")),

        Pair("AFRICA", listOf("AFRICA", "AIR", "CAR", "ARC")),
        Pair("NIGERIA", listOf("NIGERIA", "RAIN", "RAGE", "GIN")),
        Pair("LONDON", listOf("LONDON", "DON", "OLD", "NOD")),
        Pair("CANADA", listOf("CANADA", "CAN", "AND")), // Removed nonstandard/name "ADA"
        Pair("BRAZIL", listOf("BRAZIL", "RAIL", "AIR", "LAB")),
        Pair("TOKYO", listOf("TOKYO", "TOY", "TOO")), // Removed nonstandard "YOK"
        Pair("PARIS", listOf("PARIS", "PAIR", "AIR", "SAP")),
        Pair("BERLIN", listOf("BERLIN", "LINE", "BIN", "RIB")),
        Pair("DUBAI", listOf("DUBAI", "AID", "BAD", "DUB")),
        Pair("LAGOS", listOf("LAGOS", "GOAL", "GAS", "SAG")),

        Pair("BEACH", listOf("BEACH", "ACHE", "EACH", "CAB")),
        Pair("RIVER", listOf("RIVER", "RIVE", "VIE", "ERR")),
        Pair("MOUNTAIN", listOf("MOUNTAIN", "MOUNT", "ANTI", "TIN")),
        Pair("VALLEY", listOf("VALLEY", "ALLY", "LAY", "YELL")),
        Pair("DESERT", listOf("DESERT", "REST", "TREE", "RED")),
        Pair("FOREST", listOf("FOREST", "SOFT", "ROSE", "TORE")),
        Pair("ISLAND", listOf("ISLAND", "LAND", "SAIL", "SAND")),
        Pair("JUNGLE", listOf("JUNGLE", "GLUE", "LUNG", "GEL")),
        Pair("OCEAN", listOf("OCEAN", "CANE", "ONE", "CAN")),
        Pair("CASTLE", listOf("CASTLE", "LACE", "SEAT", "CAT")),

        Pair("BRIDGE", listOf("BRIDGE", "BIRD", "GRID", "RIDE")),
        Pair("MARKET", listOf("MARKET", "MAKE", "RATE", "TEAM", "TEAR")),
        Pair("VILLAGE", listOf("VILLAGE", "VIAL", "GAVE", "LIVE")),
        Pair("SCHOOL", listOf("SCHOOL", "COOL", "SOLO", "COL")),
        Pair("COLLEGE", listOf("COLLEGE", "CELL", "LOG", "LEG")),
        Pair("LIBRARY", listOf("LIBRARY", "RAIL", "AIR", "BAR")),
        Pair("HOSPITAL", listOf("HOSPITAL", "PATH", "SHIP", "POST")),
        Pair("OFFICE", listOf("OFFICE", "ICE", "OFF")), // Removed nonstandard "COFF"
        Pair("FACTORY", listOf("FACTORY", "FACTOR", "ART", "TOY")),
        Pair("STATION", listOf("STATION", "ANTI", "TON", "SIT")),

        Pair("ENGINE", listOf("ENGINE", "GENIE", "NINE", "GIN")),
        Pair("TRACTOR", listOf("TRACTOR", "ACTOR", "CAR", "ART")),
        Pair("BICYCLE", listOf("BICYCLE", "CYCLE", "ICE", "LIE")),
        Pair("AIRPORT", listOf("AIRPORT", "TRAP", "PORT", "PAIR")),
        Pair("JOURNEY", listOf("JOURNEY", "YOUR", "RUN", "JOY")),
        Pair("TICKET", listOf("TICKET", "KITE", "TIE", "ICE")),
        Pair("LUGGAGE", listOf("LUGGAGE", "GALE", "AGE", "LUG")),
        Pair("SAILOR", listOf("SAILOR", "SAIL", "SOAR", "AIR")),
        Pair("CAPTAIN", listOf("CAPTAIN", "PAINT", "ANTI", "CAP")),
        Pair("ROCKET", listOf("ROCKET", "ROCK", "TORE", "CORE")),

        Pair("PLANET", listOf("PLANET", "PLANE", "LATE", "NET")),
        Pair("GALAXY", listOf("GALAXY", "GALA", "LAY", "GAY", "LAG")),
        Pair("ASTEROID", listOf("ASTEROID", "STAR", "ROAD", "TIDE")),
        Pair("COMET", listOf("COMET", "COME", "TOE", "MET")),
        Pair("SATURN", listOf("SATURN", "TURN", "RUST", "SUN")),
        Pair("NEPTUNE", listOf("NEPTUNE", "TUNE", "PEN", "TEN")),
        Pair("MERCURY", listOf("MERCURY", "CURE", "RUM", "CRY")),
        Pair("JUPITER", listOf("JUPITER", "TRIP", "RITE", "JET")),
        Pair("VENUS", listOf("VENUS", "SUN", "USE", "SUE")),
        Pair("EARTH", listOf("EARTH", "HEAR", "HAT", "RATE")),

        Pair("BANANA", listOf("BANANA", "BAN", "NAB")), // Removed "ANA"
        Pair("ORANGE", listOf("ORANGE", "RANGE", "RAGE", "ONE")),
        Pair("PAPAYA", listOf("PAPAYA", "PAY", "PAPA", "YAP")),
        Pair("MANGO", listOf("MANGO", "MAN", "AGO")), // Removed non-standard "GOAN"
        Pair("TOMATO", listOf("TOMATO", "ATOM", "TOO", "MAT")),
        Pair("POTATO", listOf("POTATO", "TOP", "TOO", "PAT")),
        Pair("CARROT", listOf("CARROT", "CART", "TAR", "ROT")),
        Pair("CUCUMBER", listOf("CUCUMBER", "CUBE", "CURB", "BERM")),
        Pair("ONION", listOf("ONION", "ION", "NO")), // Removed "NON"
        Pair("PEPPER", listOf("PEPPER", "PEER", "PER", "REP")),

        Pair("CHICKEN", listOf("CHICKEN", "CHECK", "HEN", "INK")),
        Pair("SAUSAGE", listOf("SAUSAGE", "SAGE", "AGE", "USE")),
        Pair("BISCUIT", listOf("BISCUIT", "SUIT", "BITS", "BUS")),
        Pair("NOODLES", listOf("NOODLES", "NOSE", "DONE", "LOSE")),
        Pair("SPAGHETTI", listOf("SPAGHETTI", "SPIT", "HAT", "GET")),
        Pair("BURGER", listOf("BURGER", "RUG", "BUG", "RUB")),
        Pair("SANDWICH", listOf("SANDWICH", "SAND", "WASH", "HAND")),
        Pair("PIZZA", listOf("PIZZA", "ZIP", "PI")), // Removed "PAZ"
        Pair("CHOCOLATE", listOf("CHOCOLATE", "COAT", "LATE", "COLA")),
        Pair("ICECREAM", listOf("ICECREAM", "CREAM", "RACE", "ICE")),

        Pair("DOCTOR", listOf("DOCTOR", "CORD", "ROOT", "DOT")),
        Pair("NURSE", listOf("NURSE", "SURE", "RUN", "SUN")),
        Pair("TEACHER", listOf("TEACHER", "CHEAT", "HEAR", "TEA")),
        Pair("LAWYER", listOf("LAWYER", "WEAR", "YEAR", "LAY")),
        Pair("POLICE", listOf("POLICE", "COIL", "LICE", "ICE")),
        Pair("FARMER", listOf("FARMER", "FRAME", "ARM", "FAR")),
        Pair("PILOT", listOf("PILOT", "PLOT", "TIP", "LIP")),
        Pair("DRIVER", listOf("DRIVER", "RIDE", "DIVE")), // Removed "RIV"
        Pair("SINGER", listOf("SINGER", "SIGN", "RING", "SIN")),
        Pair("DANCER", listOf("DANCER", "DANCE", "CARE", "RED")),

        Pair("GUITAR", listOf("GUITAR", "RUG", "AIR", "TAG")),
        Pair("PIANO", listOf("PIANO", "PAIN", "NAP", "PIN")),
        Pair("DRUM", listOf("DRUM", "MUD", "RUM")), // Removed "DRU"
        Pair("TRUMPET", listOf("TRUMPET", "TRUMP", "TRUE", "PET")),
        Pair("VIOLIN", listOf("VIOLIN", "LION", "OIL", "NIL")),
        Pair("FLUTE", listOf("FLUTE", "LEFT", "FELT", "LET")),
        Pair("MUSIC", listOf("MUSIC", "SUM")), // Removed "CUS", "MUS"
        Pair("MELODY", listOf("MELODY", "MOLD", "DOME", "DEMO")),
        Pair("RHYTHM", listOf("RHYTHM", "MYTH", "TRY", "THY")),
        Pair("DANCING", listOf("DANCING", "DING", "GAIN", "CAN")),

        Pair("NAVY", listOf("NAVY", "VAN", "ANY", "NAY")), // Changed "JAVA" (which had unspellable "JAR"/"VAN") to NAVY

        Pair("ELEPHANT", listOf("ELEPHANT", "PLANT", "HEAT", "PEN")),
        Pair("GIRAFFE", listOf("GIRAFFE", "RAGE", "GEAR", "FEAR")),
        Pair("CROCODILE", listOf("CROCODILE", "COIL", "CODE", "RIDE")),
        Pair("KANGAROO", listOf("KANGAROO", "KANGA", "ROAN", "RANG")),
        Pair("DOLPHIN", listOf("DOLPHIN", "HOLD", "LION", "HOP")),
        Pair("BUTTERFLY", listOf("BUTTERFLY", "FLY", "TRUE", "BUTTER")),
        Pair("MOSQUITO", listOf("MOSQUITO", "MOST", "QUIT", "SIT")),
        Pair("CHEETAH", listOf("CHEETAH", "HEAT", "CHAT", "HATE")),
        Pair("PENGUIN", listOf("PENGUIN", "PEN", "GUN", "NINE")),
        Pair("SQUIRREL", listOf("SQUIRREL", "SURE", "RULE", "RISE"))
    )

    val levelNames = listOf(
        "Sunrise Valley", "Sunny Meadow", "Sandy Shore", "Forest Trail", "Misty Peaks",
        "Deep Ocean", "Desert Oasis", "Chilly Lake", "Green Field", "Cosmic Sky",
        "Golden Coast", "Volcano Rock", "Enchanted Woods", "Whispering Winds", "Crystal Cave",
        "Starlight Summit", "Autumn Breeze", "Shadowy Path", "Rainbow Falls", "Serene Oasis",
        "Dewy Grasslands", "Glimmering Grove", "Ancient Ruins", "Echoing Ravine", "Prismatic Lagoon",
        "Celestial Canopy", "Ember Valley", "Frozen Tundra", "Whispering Willow", "Mystic Garden",
        "Lotus Pond", "Saffron Steppes", "Jade Rainforest", "Silver Glacier", "Twilight Ridge"
    )
}
