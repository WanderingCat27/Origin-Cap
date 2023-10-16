# Origin-Cap
### Addon to Origins mod, unaffiliated
<br>
<p>Adds to ability to cap origins so only x number of players can pick any specific origin</p>
<br>

<p> <b> Default cap is 3, can be changed with commands </b></p>
<br>

<p> use /cap help for gudie to mod commands altho the implementation of help was thrown together and is hard to read </p>
<br>

<p> I suggest just reading here: </p>
<br>

/cap 
- clear (clearing removes the cap data from this mod but does not force players to rechoose their origin) 
    - all (resets entire cap) 
    - layer (resets just one layer of cap) 
    - origin (resets a specfic origin on a specific layer) 
- ignore (A blacklist of sorts  allows anyone to choose from the origin or layer 
    - layer (blacklist a layer from being capped) 
    - origin (blacklist an origin from being capped) 
- overrideMax (Allows for you to specify the maximum number of players can choose globally  per layer  or per origin) 
    - set (set the override for each of the following) 
        - globalDef (max for all non overrided layers and origins) 
        - layer (per layer)  
        - origin (per origin) 
    - remove (remove an override setting it to its parents default) 
	    - layer (returns the layer to follow the global def) 
	    - origin (returns the origin to follow its layer if the layer overrides or the global max) 
	- printOverrides (prints your changes and overrides) 
- print (prints the state of the cap  uses uuids of players) 
- removePlayer (Clear a player from the cap) 
	- offline (if the player is offline  the mod attempts to fetch the uuid of the player from mojang servers  so type their username in as an argument) 
	- online (if the player is on the server  you can just type their username with autocomplete)



