# ExcellentEnchants Folia Migration Plan

This document outlines the tasks required to port ExcellentEnchants to Folia using FoliaLib as a compatibility layer.

## 1. Project Setup
- [x] Add FoliaLib dependency to pom.xml (with proper relocation)
- [x] Keep Paper API dependency for base functionality
- [x] Create test environment with both Paper and Folia servers
- [x] Update maven-shade-plugin to 3.5.1
- [x] Add FoliaLib relocation to su.nightexpress.excellentenchants.lib.folialib

## 2. Code Analysis
- [ ] Identify all scheduler usages (BukkitScheduler/BukkitRunnable)
- [ ] Locate direct world access code
- [ ] List all event listeners that might need region-aware handling
- [ ] Check for cross-world operations
- [ ] Identify entity-specific scheduled tasks

## 3. Core Changes
- [ ] Replace BukkitScheduler with FoliaLib's scheduler
  - [ ] Replace `runNextTick()` calls
  - [ ] Replace async tasks with `runAsync()`
  - [ ] Replace timer tasks with `runTimer()`/`runTimerAsync()`
  - [ ] Update entity-specific tasks to use `runAtEntity()`
  - [ ] Update location-specific tasks to use `runAtLocation()`
- [ ] Update teleportation code to use `teleportAsync()`
- [ ] Modify particle effects to use region-aware scheduling
- [ ] Add proper task cleanup in onDisable()

## 4. Enchantment-Specific Updates
- [ ] Review each enchantment's effect implementation
- [ ] Update area-of-effect enchantments to use `runAtLocation()`
- [ ] Modify entity-targeting enchantments to use `runAtEntity()`
- [ ] Update enchantments that create persistent effects
- [ ] Review enchantments that modify world (blocks/environment)

## 5. API Updates
- [ ] Create utility wrapper around FoliaLib for common operations
- [ ] Add compatibility methods for region-aware operations
- [ ] Update API documentation with FoliaLib usage examples
- [ ] Add platform-specific checks where needed using `foliaLib.isFolia()`

## 6. Testing
- [ ] Test each enchantment on Paper server
- [ ] Test each enchantment on Folia server
- [ ] Test cross-region enchantment effects
- [ ] Performance testing with multiple regions
- [ ] Verify task cleanup works correctly

## 7. Documentation & Release
- [ ] Update README with Folia compatibility notes
- [ ] Document FoliaLib integration
- [ ] Update configuration examples
- [ ] Create migration guide for server admins
- [ ] Update version numbering to reflect Folia support

## Notes
- FoliaLib provides a unified API that works on both Paper and Folia
- No need to check for platform type in most cases, FoliaLib handles it
- Key FoliaLib methods to use:
  - `runNextTick()` - For global tasks
  - `runAsync()` - For async operations
  - `runAtLocation()` - For location-specific tasks
  - `runAtEntity()` - For entity-specific tasks
  - `teleportAsync()` - For safe teleportation
  - `runTimer()`/`runTimerAsync()` - For repeating tasks
- Remember to call `cancelAllTasks()` in onDisable()

# Folia Migration Checklist

## 1. Scheduler Updates

### AnvilListener.java
- [x] Replace `plugin.runTask()` with `FoliaLibWrapper.runNextTick()`

### GenericListener.java
- [x] Replace `plugin.runTask()` with `FoliaLibWrapper.runAtEntity()` for player inventory updates
- [x] Replace `plugin.runTask()` with `FoliaLibWrapper.runAtEntity()` for other player-related tasks

### AutoReelEnchant.java
- [x] Replace `plugin.runTask()` with `FoliaLibWrapper.runAtEntity()`

### CurseOfFragilityEnchant.java
- [x] Replace `plugin.runTask()` with `FoliaLibWrapper.runAtEntity()`

### SoulboundEnchant.java
- [x] Replace `plugin.runTask()` with `FoliaLibWrapper.runAtEntity()`

### ReplanterEnchant.java
- [x] Replace `plugin.runTask()` with `FoliaLibWrapper.runAtLocation()`

### StoppingForceEnchant.java
- [x] Replace `plugin.runTask()` with `FoliaLibWrapper.runAtEntity()`

## 2. World Access Updates

### EnchantManager.java
- [ ] Update world iteration to use Folia-compatible methods
- [ ] Replace `getServer().getWorlds()` with region-aware iteration

### GameEnchantment.java
- [ ] Update world availability checks to be region-aware

## 3. Entity Operations

### CureEnchant.java
- [ ] Update entity spawning to use Folia-compatible methods

### CurseOfDrownedEnchant.java
- [ ] Update entity spawning to use Folia-compatible methods

### DecapitatorEnchant.java
- [ ] Update item dropping to use Folia-compatible methods

### CutterEnchant.java
- [ ] Update item dropping to use Folia-compatible methods

## 4. Block Operations

### SilkChestEnchant.java
- [ ] Update block state changes to use Folia-compatible methods
- [ ] Update inventory operations to be region-aware

### SmelterEnchant.java
- [ ] Update block state changes to use Folia-compatible methods

### ReplanterEnchant.java
- [ ] Update block state changes to use Folia-compatible methods

### BlastMiningEnchant.java
- [ ] Update explosion creation to use Folia-compatible methods

## 5. Location-based Operations

### ThunderEnchant.java
- [ ] Update lightning effects to use Folia-compatible methods
- [ ] Update block light checks to be region-aware

### RocketEnchant.java
- [ ] Update firework spawning to use Folia-compatible methods

### IceAspectEnchant.java
- [ ] Update sound effects to use Folia-compatible methods

## Migration Progress
- [ ] 1. Scheduler Updates
- [ ] 2. World Access Updates
- [ ] 3. Entity Operations
- [ ] 4. Block Operations
- [ ] 5. Location-based Operations

## Notes
- All scheduler operations should use `FoliaLibWrapper` methods
- Entity operations should be performed on the correct region
- Block operations should be performed on the correct region
- Location-based operations should use region-aware methods
- Sound effects should be played using region-aware methods

## Event Handling
- [ ] Verify all event handlers are thread-safe
- [ ] Update any event handlers that modify world state to use FoliaLib
- [ ] Test event handling on Folia server

## NMS Handling
- [ ] Verify NMS code is thread-safe
- [ ] Update any NMS code that modifies world state to use FoliaLib
- [ ] Test NMS functionality on Folia server

## Registry Management
- [ ] Verify enchantment registries are thread-safe
- [ ] Update any registry operations that modify world state to use FoliaLib
- [ ] Test registry operations on Folia server

## Testing
- [ ] Test on Folia server
- [ ] Verify all enchantments work correctly
- [ ] Verify all commands work correctly
- [ ] Verify all events work correctly
- [ ] Verify all NMS operations work correctly
- [ ] Verify all registry operations work correctly 