package meranha.mekaweapons;

import mekanism.api.MekanismIMC;
import mekanism.api.providers.IModuleDataProvider;
import mekanism.client.ClientRegistrationUtil;
import mekanism.common.Mekanism;
import mekanism.common.config.MekanismConfigHelper;
import mekanism.common.item.ItemModule;
import mekanism.common.registration.impl.*;
import mekanism.common.registries.MekanismItems;
import mekanism.common.registries.MekanismModules;
import meranha.mekaweapons.items.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@SuppressWarnings({"Convert2MethodRef", "unused", "forremoval"})
@Mod.EventBusSubscriber(modid = MekaWeapons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@Mod(MekaWeapons.MODID)
public class MekaWeapons {
    public static final String MODID = "mekaweapons";
    public static final WeaponsConfig general = new WeaponsConfig();

    public static final ModuleDeferredRegister MODULES =  new ModuleDeferredRegister(MekaWeapons.MODID);
    public static final ModuleRegistryObject<?> ARROWENERGY_UNIT = MODULES.registerMarker("arrowenergy_unit", () -> MekaWeapons.MODULE_ARROWENERGY.asItem(), builder -> builder.rarity(Rarity.RARE));
    public static final ModuleRegistryObject<?> AUTOFIRE_UNIT = MODULES.registerMarker("autofire_unit", () -> MekaWeapons.MODULE_AUTOFIRE.asItem(), builder -> builder.rarity(Rarity.RARE));
    public static final ModuleRegistryObject<?> DRAWSPEED_UNIT = MODULES.registerMarker("drawspeed_unit", () -> MekaWeapons.MODULE_DRAWSPEED.asItem(), builder -> builder.maxStackSize(3).rarity(Rarity.RARE));
    public static final ModuleRegistryObject<?> GRAVITYDAMPENER_UNIT = MODULES.registerMarker("gravitydampener_unit", () -> MekaWeapons.MODULE_GRAVITYDAMPENER.asItem(), builder -> builder.rarity(Rarity.EPIC));
    //public static final ModuleRegistryObject<?> ARROWVELOCITY_UNIT = MODULES.registerMarker("arrowvelocity_unit", () -> MekaWeapons.MODULE_ARROWVELOCITY.asItem(), builder -> builder.maxStackSize(8).rarity(Rarity.RARE));

    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister(MekaWeapons.MODID);
    public static final ItemRegistryObject<ItemMekaTana> MEKA_TANA = ITEMS.registerUnburnable("mekatana", ItemMekaTana::new);
    public static final ItemRegistryObject<ItemMekaBow> MEKA_BOW = ITEMS.registerUnburnable("mekabow", ItemMekaBow::new);
    public static final ItemRegistryObject<Item> MAGNETIZER = ITEMS.registerUnburnable("magnetizer", ItemMagnetizer::new);
    public static final ItemRegistryObject<Item> KATANA_BLADE = ITEMS.register("katana_blade");
    public static final ItemRegistryObject<Item> BOW_RISER = ITEMS.register("bow_riser");
    public static final ItemRegistryObject<Item> BOW_LIMB = ITEMS.register("bow_limb");
    public static final ItemRegistryObject<ItemModule> MODULE_ARROWENERGY = ITEMS.registerModule(MekaWeapons.ARROWENERGY_UNIT);
    public static final ItemRegistryObject<ItemModule> MODULE_AUTOFIRE = ITEMS.registerModule(MekaWeapons.AUTOFIRE_UNIT);
    public static final ItemRegistryObject<ItemModule> MODULE_DRAWSPEED = ITEMS.registerModule(MekaWeapons.DRAWSPEED_UNIT);
    public static final ItemRegistryObject<ItemModule> MODULE_GRAVITYDAMPENER = ITEMS.registerModule(MekaWeapons.GRAVITYDAMPENER_UNIT);
    //public static final ItemRegistryObject<ItemModule> MODULE_ARROWVELOCITY = ITEMS.registerModule(MekaWeapons.ARROWVELOCITY_UNIT);

    public static final EntityTypeDeferredRegister ENTITY_TYPES = new EntityTypeDeferredRegister(MekaWeapons.MODID);
    public static final EntityTypeRegistryObject<MekaArrowEntity> MEKA_ARROW = ENTITY_TYPES.register("meka_arrow", EntityType.Builder.<MekaArrowEntity>of(MekaArrowEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20));

    public static final String ADD_MEKA_BOW_MODULES = "add_meka_bow_modules";
    public static final String ADD_MEKATANA_MODULES = "add_mekatana_modules";

    public MekaWeapons() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MekaWeapons.ITEMS.register(modEventBus);
        MekaWeapons.MODULES.register(modEventBus);
        MekaWeapons.ENTITY_TYPES.register(modEventBus);
        MekanismConfigHelper.registerConfig(ModLoadingContext.get().getActiveContainer(), general);
        modEventBus.addListener(this::enqueueIMC);
        modEventBus.addListener(this::registerRenderers);
    }

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("curios")) {
            CuriosRendererRegistry.register(MekaWeapons.MAGNETIZER.get(), WeaponsRenderer::new);
        }
        event.enqueueWork(() -> {
            ClientRegistrationUtil.setPropertyOverride(MekaWeapons.MEKA_BOW, Mekanism.rl("pull"), (stack, world, entity, seed) -> {
                if (entity != null && entity.getUseItem() == stack && stack.getItem() instanceof ItemMekaBow bow) {
                    return (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / bow.getUseTick(stack);
                }
                return 0;
            });
            ClientRegistrationUtil.setPropertyOverride(MekaWeapons.MEKA_BOW, Mekanism.rl("pulling"), (stack, world, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }

    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MekaWeapons.MEKA_ARROW.get(), MekaArrowRenderer::new);
    }
    private void enqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder("magnetizer").icon(new ResourceLocation("curios:slot/magnetizer_slot")).build());
        addMekaBowModules(MekanismModules.ENERGY_UNIT, MekanismModules.ATTACK_AMPLIFICATION_UNIT, MekaWeapons.AUTOFIRE_UNIT, MekaWeapons.ARROWENERGY_UNIT, MekaWeapons.DRAWSPEED_UNIT, MekaWeapons.GRAVITYDAMPENER_UNIT); // MekaWeapons.ARROWVELOCITY_UNIT
        addMekaTanaModules(MekanismModules.ENERGY_UNIT, MekanismModules.ATTACK_AMPLIFICATION_UNIT, MekanismModules.TELEPORTATION_UNIT);
    }

    private static void sendModuleIMC(String method, IModuleDataProvider<?>... moduleDataProviders) {
        if (moduleDataProviders == null || moduleDataProviders.length == 0) {
            throw new IllegalArgumentException("No module data providers given.");
        }
        InterModComms.sendTo(Mekanism.MODID, method, () -> moduleDataProviders);
    }

    public static void addMekaBowModules(IModuleDataProvider<?>... moduleDataProviders) {
        sendModuleIMC(MekaWeapons.ADD_MEKA_BOW_MODULES, moduleDataProviders);
    }
    public static void addMekaTanaModules(IModuleDataProvider<?>... moduleDataProviders) {
        sendModuleIMC(MekaWeapons.ADD_MEKATANA_MODULES, moduleDataProviders);
    }
}
