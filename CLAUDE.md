# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ExcellentEnchants is a Minecraft plugin that adds 75+ custom enchantments to Spigot/Paper servers. The plugin is designed to integrate seamlessly with vanilla Minecraft mechanics while providing extensive customization options.

## Build Commands

This is a Maven-based project. Use these commands:

```bash
# Build the entire project
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Build only the Core module
mvn clean package -pl Core

# Install dependencies locally
mvn clean install

# Shade the final JAR (outputs to /target/ExcellentEnchants-{version}.jar)
mvn clean package -pl Core
```

The main output JAR is generated in the `target/` directory at the root level.

## Project Structure

This is a multi-module Maven project:

- **API/**: Public API interfaces and data structures
- **Core/**: Main plugin implementation, enchantment logic, and command handling
- **NMS/**: Version-agnostic NMS (Net Minecraft Server) interfaces
- **MC_1_21_4/**, **MC_1_21_5/**, **MC_1_21_7/**: Version-specific NMS implementations
- **target/**: Build output directory

## Key Architecture Components

### Core Plugin Structure

- **EnchantsPlugin**: Main plugin class extending NightPlugin
- **EnchantsAPI**: Static API entry point for other plugins
- **EnchantManager**: Central manager for all enchantment operations and event handling
- **EnchantRegistry**: Registry system for managing enchantment types and holders

### Enchantment System Architecture

The plugin uses a component-based architecture:

- **CustomEnchantment**: Base interface for all custom enchantments
- **EnchantComponent**: Modular components (Charges, Probability, Effects, etc.)
- **EnchantHolder**: Type-safe containers for different enchantment categories
- **GameEnchantment**: Concrete implementation in Core module

### Enchantment Categories

Enchantments are organized into logical categories:

- **armor/**: Armor-specific enchantments (e.g., FireShield, IceShield)
- **bow/**: Ranged weapon enchantments (e.g., ExplosiveArrows, EnderBow)
- **fishing/**: Fishing rod enchantments (e.g., DoubleCatch, AutoReel)
- **tool/**: Tool enchantments (e.g., Veinminer, Telekinesis)
- **universal/**: Universal enchantments (e.g., SelfRepairing, Soulbound)
- **weapon/**: Melee weapon enchantments (e.g., Vampire, Thunder)

### Configuration System

- **ConfigBridge**: Handles configuration loading and platform detection
- **DistributionConfig**: Manages enchantment distribution settings
- **EnchantDefinition**: Defines enchantment properties and behavior
- **EnchantDistribution**: Controls how enchantments appear in the world

## Folia Compatibility

The plugin is migrating to Folia support using FoliaLib. Key considerations:

- Use `FoliaLibWrapper` methods instead of direct Bukkit scheduler
- Entity-specific tasks should use `runAtEntity()`
- Location-specific tasks should use `runAtLocation()`
- Async operations should use `runAsync()`
- See FOLIA_MIGRATION.md for detailed migration progress

## Version Support

- **Java**: 21 or higher
- **Server**: Spigot/Paper/Purpur 1.21.4+
- **Dependencies**: nightcore 2.7.9+, optional ProtocolLib/PacketEvents

## Development Guidelines

### Adding New Enchantments

1. Create the enchantment class in the appropriate category package
2. Implement the relevant enchantment type interface (e.g., `AttackEnchant`, `MiningEnchant`)
3. Register the enchantment in `EnchantProviders.load()`
4. Add configuration files in the appropriate directory
5. Test with both Paper and Folia servers

### Code Patterns

- Enchantments use components for modular functionality
- Event handling is centralized in `EnchantManager` and specific listeners
- NMS operations are abstracted through version-specific implementations
- Configuration is file-based with automatic reloading support

### Testing

- Test all enchantments on both Paper and Folia servers
- Verify cross-region functionality for area-of-effect enchantments
- Test with various protection plugins and compatibility layers
- Ensure proper cleanup of scheduled tasks and temporary effects

## Common Issues

- **Merge Conflicts**: The project shows git merge conflicts in several files, particularly in `EnchantsPlugin.java`
- **Version Compatibility**: Ensure NMS implementations match the target server versions
- **Thread Safety**: All operations must be thread-safe for Folia compatibility
- **Resource Cleanup**: Always clean up scheduled tasks and temporary data structures

## Important Files

- `Core/src/main/java/su/nightexpress/excellentenchants/EnchantsPlugin.java`: Main plugin entry point
- `API/src/main/java/su/nightexpress/excellentenchants/api/EnchantRegistry.java`: Central registry system
- `Core/src/main/java/su/nightexpress/excellentenchants/manager/EnchantManager.java`: Core enchantment management
- `FOLIA_MIGRATION.md`: Detailed Folia migration progress and checklist