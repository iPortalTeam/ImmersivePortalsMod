Note: this version is preview and not yet stable. It's recommended to backup the world before using it.

Upgrade to MC 1.20.2 .

Added command `/dims add_dimension` that allows adding new dimension based on several dimension templates: `void`, `bright_void`, `skyland`, `bright_skyland`, `chaos`.

Added command `/dims view_dim_config` to view a dimension's config.

Now, alternate dimensions will only be added if they are used in dimension stack. The dimension id of the new alternate dimensions has been changed to more meaningful id such as `immersive_portals:skyland` (instead of the old `alternate1` `alternate2`).

Dimension API now gets refactored. The same method can be used to both register dimension during server start and dynamically add dimension when server is running. The extra dimension storage on `q_dimension_configs` folder is now not being used (now stored in `level.dat` which is the vanilla way).

Immersive Portals items in creative mode inventory are now in a new tab.

Added mod version sync to check if the server and client have the same mod version.

Also refactored networking, entity tracking and portal custom shape systems.