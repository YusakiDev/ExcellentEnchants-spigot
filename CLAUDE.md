# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ExcellentEnchants is a sophisticated Minecraft server plugin that adds 75+ custom enchantments to Spigot/Paper/Purpur/Folia servers. It's a multi-module Maven project written in Java 21, featuring a component-based architecture with version-specific NMS implementations.

## Build Commands

- **Full build**: `mvn clean install` - Builds all modules and creates the final shaded JAR
- **Package only**: `mvn clean package` - Creates distribution JAR without installing to local repository
- **Final JAR location**: `target/ExcellentEnchants-{version}.jar` (root directory)

**Requirements**: Java 21, Maven (no wrapper provided)

## Module Architecture

The project uses a 6-module structure:

- **API**: Public interfaces and contracts (`su.nightexpress.excellentenchants.api.*`)
- **Core**: Main implementation with all enchantment logic and plugin lifecycle
- **NMS**: Base NMS abstraction layer
- **MC_1_21_4**, **MC_1_21_5**, **MC_1_21_7**: Version-specific Minecraft server implementations

## Key Architectural Patterns

### Dual Initialization System
- **EnchantsBootstrap**: Handles enchantment registration during server bootstrap (Paper-specific)
- **EnchantsPlugin**: Main plugin class extending NightCore framework for runtime management

### Component-Based Enchantments
Enchantments use composable components via `EnchantComponent<T>`:
- `PROBABILITY`: Chance-based triggering
- `CHARGES`: Limited-use functionality  
- `PERIODIC`: Time-based effects
- `POTION_EFFECT`: Status effect application
- `ARROW`: Projectile-specific behaviors

### Registry System
- **EnchantRegistry**: Central registry with data, provider, and holder layers
- **Type-safe categorization**: `EnchantHolder<T>` provides compile-time safety
- **Priority system**: `EnchantPriority` controls execution order for predictable behavior

### Version Compatibility
NMS implementations handle Minecraft version compatibility through:
- `RegistryHack` interface for runtime enchantment registration
- Reflection-based access for deep Bukkit integration
- Version-specific modules isolate breaking changes

## Important Base Classes

- **GameEnchantment**: Abstract base for all custom enchantments with common functionality
- **CustomEnchantment**: Core interface defining enchantment contracts
- **EnchantManager**: Orchestrates event handling and enchantment execution
- **ConfigBridge**: Centralized configuration access pattern

## Core Directories

**Enchantment implementations** (Core module):
- `/enchantment/armor/` - 16 armor enchantments
- `/enchantment/bow/` - 15 bow enchantments  
- `/enchantment/weapon/` - 25 weapon enchantments
- `/enchantment/tool/` - 12 tool enchantments
- `/enchantment/fishing/` - 6 fishing enchantments
- `/enchantment/universal/` - 6 universal enchantments

**System management**:
- `/manager/` - Core systems (listeners, damage, blocks, menus)
- `/config/` - Configuration classes and language files
- `/hook/` - Integration with ProtocolLib, PlaceholderAPI, MythicMobs

## Configuration System

- **File-based**: Each enchantment has individual config files
- **DistributionConfig**: Controls where enchantments appear (villagers, loot tables, etc.)
- **ItemSetRegistry**: Manages item compatibility for enchantments
- **Hot-reloading**: Configuration can be reloaded without restart

## Event Architecture

Three main listener classes handle different aspects:
- **EnchantListener**: Core enchantment triggering logic
- **AnvilListener**: Anvil repair and combining mechanics
- **GenericListener**: General game integration

Event processing includes component-aware handling for charges, probability calculations, and cooldown management.

## Development Notes

- **No testing framework** - Manual testing required
- **NightCore dependency**: Required framework for common functionality
- **Shaded dependencies**: FoliaLib is relocated to prevent conflicts
- **Multi-server support**: Designed for Spigot, Paper, Purpur, and Folia compatibility
- **Performance optimizations**: Async processing for effects, concurrent collections, caching strategies