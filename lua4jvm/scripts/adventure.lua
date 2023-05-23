-- Game written by OpenAI's GPT-4 and cleaned up by me (by hand)
-- Prompt details (temperature 1):
-- SYSTEM: "You are a creative game programming assistant."
-- USER: "Write me a short command-line text adventure game in Lua. Start from a forest glade."
-- USER: "Give me a version of 'places' array with 10 interconnected places."

game_over = false

-- A table to store different places and their descriptions
places = {
    forest_glade = {
        name = "Forest Glade",
        description = "You are in a beautiful forest glade, surrounded by tall trees and green plants. There's a path leading to the north and east.",
        north = "little_village",
        east = "deep_forest"
    },
    little_village = {
        name = "Little Village",
        description = "You reach a charming little village. The villagers are friendly, and their houses are quaint. You can go back to the forest glade by going south, or visit the mountain trail to the east.",
        south = "forest_glade",
        east = "mountain_trail"
    },
    deep_forest = {
        name = "Deep Forest",
        description = "You have entered the deep forest. The trees are so tall that they almost obscure the sky, and it becomes darker. You can go back to the forest glade by going west, and east again to reach a mysterious cave.",
        west = "forest_glade",
        east = "mysterious_cave"
    },
    mountain_trail = {
        name = "Mountain Trail",
        description = "You are on a mountain trail where the air is thin and the view is breathtaking. A narrow path leads north to an ancient fortress.",
        north = "ancient_fortress",
        west = "little_village"
    },
    mysterious_cave = {
        name = "Mysterious Cave",
        description = "The mysterious cave is dark and damp. You feel a cold chill run down your spine, but you see a faint glow coming from the north.",
        north = "glowing_chamber",
        west = "deep_forest"
    },
    ancient_fortress = {
        name = "Ancient Fortress",
        description = "The ancient fortress stands tall and imposing, its walls weathered by countless years. A lonely tower is to the east.",
        east = "lonely_tower",
        south = "mountain_trail"
    },
    glowing_chamber = {
        name = "Glowing Chamber",
        description = "The chamber illuminates with a strange glow, emanating from the crystals on the walls. A small opening to the east leads to an underground river.",
        east = "underground_river",
        south = "mysterious_cave"
    },
    lonely_tower = {
        name = "Lonely Tower",
        description = "The lonely tower seems to have been abandoned for ages, yet there's a cozy little living space inside. A balcony faces the west, overlooking the ancient fortress.",
        west = "ancient_fortress"
    },
    underground_river = {
        name = "Underground River",
        description = "The underground river flows quietly, providing a serene atmosphere. A narrow passage to the west leads back to the glowing chamber, while another passage to the south leads to a hidden sanctuary.",
        west = "glowing_chamber",
        south = "hidden_sanctuary"
    },
    hidden_sanctuary = {
        name = "Hidden Sanctuary",
        description = "The hidden sanctuary is a peaceful area bathed in soft light streaming through a crack in the ceiling. A beautiful altar is adorned with flowers and offerings.",
        north = "underground_river"
    }
}

function print_description(place)
    print("[" .. place.name .. "]")
    print(place.description)
end

function change_place(new_place)
    current_place = places[new_place]
    print_description(current_place)
end

function process_input()
    local command = code4jvm.read()

    if command == "quit" then
        game_over = true
    elseif command == "north" or command == "south" or command == "east" or command == "west" then
        local new_place = current_place[command]
        if new_place ~= nil then
            change_place(new_place)
        else
            print("You can't go that way.")
        end
    else
        print("Invalid command. Type 'north', 'south', 'east', 'west', or 'quit'.")
    end
end

print("Welcome to the Text Adventure Game!")
change_place("forest_glade")

-- Main game loop
while game_over == false do
    process_input()
end

print("Thank you for playing!")