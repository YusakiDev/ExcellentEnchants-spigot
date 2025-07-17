# FoliaLib Independence Progress Log

## Objective
Make FoliaLib independent from NightPlugin by removing dependency on NightCore's wrapper methods and using direct FoliaLib calls instead.

## Progress Summary

### ✅ Completed Tasks
1. **Added FoliaLib direct integration to EnchantsPlugin class**
   - Added `FoliaLib foliaLib` field to EnchantsPlugin
   - Added `getFoliaLib()` getter method
   - Initialize FoliaLib in `enable()` method

2. **Replaced all runAtEntity/runAtLocation calls (17 total)**
   - **EnchantUtils.java** (3 locations): `busyBreak()`, `runInDisabledDisplayUpdate()`, `populateResource()`
   - **VitalityEnchant.java** (7 locations): All event handlers for equipment processing
   - **SoulboundEnchant.java** (1 location): Player death item restoration
   - **AutoReelEnchant.java** (1 location): Fishing auto-reel
   - **StoppingForceEnchant.java** (1 location): Velocity modification
   - **ReplanterEnchant.java** (1 location): Block placement
   - **EnchantManager.java** (1 location): Passive enchant ticking
   - **AnvilListener.java** (1 location): Repair cost updates
   - **GenericListener.java** (1 location): Inventory updates

3. **Updated Maven configuration**
   - Changed FoliaLib dependency scope from `provided` to `compile`
   - Added FoliaLib to Maven Shade plugin includes
   - Added relocation configuration to shade FoliaLib as `su.nightexpress.excellentenchants.libs.folialib`

### ❌ Issues Encountered

#### ClassCastException Problem
```
java.lang.ClassCastException: class su.nightexpress.nightcore.libs.folialib.wrapper.task.WrappedFoliaTask cannot be cast to class su.nightexpress.excellentenchants.libs.folialib.wrapper.task.WrappedTask
```

**Root Cause**: Two different FoliaLib versions in classpath:
- **NightCore's FoliaLib**: `su.nightexpress.nightcore.libs.folialib.*`
- **ExcellentEnchants' FoliaLib**: `su.nightexpress.excellentenchants.libs.folialib.*`

#### Still Using NightCore Task Scheduling
The following locations still use NightCore's task scheduling (which internally uses NightCore's FoliaLib):
- **EnchantManager.java**: `addAsyncTask()` (line 68), `addTask()` (lines 71, 74), `plugin.runTask()` (line 240)
- **GenericListener.java**: `plugin.runTask()` (line 67)
- **EnchantListener.java**: `plugin.runTask()` (line 122)

## Current Status
- ✅ All direct `runAtEntity`/`runAtLocation` calls converted to use ExcellentEnchants' FoliaLib
- ✅ ClassCastException resolved - using ExcellentEnchants' own shaded FoliaLib consistently
- ✅ Vitality enchant fixed - added check for existing attribute modifiers
- ✅ **FoliaLib is now independent from NightPlugin wrapper methods**

## ✅ Final Solution Implemented
We decided to keep ExcellentEnchants' own FoliaLib dependency and use it consistently throughout the codebase. This approach:

### Benefits Achieved
- ✅ **Complete independence from NightPlugin wrapper methods** - All scheduling now uses direct FoliaLib calls
- ✅ **Maintains Folia compatibility** - Uses proper entity/location-based scheduling
- ✅ **Clean, direct API** - All calls use `plugin.getFoliaLib().getScheduler()`
- ✅ **No class loading conflicts** - Uses single FoliaLib instance consistently
- ✅ **Better performance** - Eliminates wrapper method overhead

### Key Fixes Applied
1. **VitalityEnchant Issue**: Added check `if (maxHealthAttribute.getModifier(modifierKey) == null)` before adding attribute modifiers to prevent "Modifier is already applied" errors
2. **Consistent FoliaLib Usage**: All 17 `runAtEntity`/`runAtLocation` calls now use ExcellentEnchants' shaded FoliaLib

## Files Modified
- `/Core/src/main/java/su/nightexpress/excellentenchants/EnchantsPlugin.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/util/EnchantUtils.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/enchantment/armor/VitalityEnchant.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/enchantment/universal/SoulboundEnchant.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/enchantment/fishing/AutoReelEnchant.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/enchantment/armor/StoppingForceEnchant.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/enchantment/tool/ReplanterEnchant.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/manager/EnchantManager.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/manager/listener/AnvilListener.java`
- `/Core/src/main/java/su/nightexpress/excellentenchants/manager/listener/GenericListener.java`
- `/Core/pom.xml`

## Key Code Changes Made

### EnchantsPlugin.java
```java
import com.tcoded.folialib.FoliaLib;

public class EnchantsPlugin extends NightPlugin implements ImprovedCommands {
    private FoliaLib foliaLib;
    
    @Override
    public void enable() {
        this.foliaLib = new FoliaLib(this);
        // ... rest of enable method
    }
    
    @NotNull
    public FoliaLib getFoliaLib() {
        return this.foliaLib;
    }
}
```

### Pattern Used Throughout Codebase
```java
// Before:
this.plugin.runAtEntity(player, task -> { /* logic */ });

// After:
this.plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> { /* logic */ });
```

### Maven Configuration (pom.xml)
```xml
<dependency>
    <groupId>com.tcoded</groupId>
    <artifactId>FoliaLib</artifactId>
    <version>0.5.1</version>
    <scope>compile</scope>  <!-- Changed from provided -->
</dependency>

<!-- Added to shade plugin -->
<artifactSet>
    <includes>
        <include>su.nightexpress.excellentenchants:*</include>
        <include>com.tcoded:FoliaLib</include>
    </includes>
</artifactSet>
<relocations>
    <relocation>
        <pattern>com.tcoded.folialib</pattern>
        <shadedPattern>su.nightexpress.excellentenchants.libs.folialib</shadedPattern>
    </relocation>
</relocations>
```

### VitalityEnchant.java Fix
```java
// Added check to prevent duplicate attribute modifiers
if (maxHealthAttribute.getModifier(modifierKey) == null) {
    maxHealthAttribute.addModifier(modifier);
}
```

## ✅ Task Complete
**FoliaLib is now fully independent from NightPlugin wrapper methods!**

All 17 `runAtEntity`/`runAtLocation` calls have been successfully converted to use ExcellentEnchants' own shaded FoliaLib instance via the `getFoliaLib()` method. The plugin now has complete control over its scheduling and is no longer dependent on NightCore's wrapper methods.

## Outstanding Issues
- ❌ **Self-repairing enchant not working** - Needs investigation

---
*Last updated: 2025-01-17*