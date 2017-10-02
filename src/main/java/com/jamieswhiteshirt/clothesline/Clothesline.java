package com.jamieswhiteshirt.clothesline;

import com.jamieswhiteshirt.clothesline.api.IAttacher;
import com.jamieswhiteshirt.clothesline.api.INetworkManager;
import com.jamieswhiteshirt.clothesline.common.ClotheslineBlocks;
import com.jamieswhiteshirt.clothesline.common.CommonProxy;
import com.jamieswhiteshirt.clothesline.common.block.BlockClotheslineAnchor;
import com.jamieswhiteshirt.clothesline.common.capability.*;
import com.jamieswhiteshirt.clothesline.common.item.ItemClothesline;
import com.jamieswhiteshirt.clothesline.common.network.message.MessageSyncNetworks;
import com.jamieswhiteshirt.clothesline.common.tileentity.TileEntityClotheslineAnchor;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(
        modid = Clothesline.MODID,
        version = Clothesline.VERSION,
        name = "Clothesline"
)
public class Clothesline {
    public static final String MODID = "clothesline";
    public static final String VERSION = "0.0.0.0";

    @CapabilityInject(INetworkManager.class)
    public static Capability<INetworkManager> NETWORK_MANAGER_CAPABILITY;

    @Mod.Instance
    public static Clothesline instance;
    @SidedProxy(
            clientSide = "com.jamieswhiteshirt.clothesline.client.ClientProxy",
            serverSide = "com.jamieswhiteshirt.clothesline.server.ServerProxy",
            modId = MODID
    )
    public static CommonProxy proxy;

    public final SimpleNetworkWrapper networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        CapabilityManager.INSTANCE.register(INetworkManager.class, new NetworkManagerStorage(), NetworkManager.class);
        CapabilityManager.INSTANCE.register(IAttacher.class, new AttacherStorage(), Attacher::new);
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(
                new BlockClotheslineAnchor().setUnlocalizedName("clothesline.clotheslineAnchor").setRegistryName(MODID, "clothesline_anchor")
        );

        GameRegistry.registerTileEntity(TileEntityClotheslineAnchor.class, "clothesline:clothesline_anchor");
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new ItemClothesline().setUnlocalizedName("clothesline.clothesline").setRegistryName(MODID, "clothesline"),
                new ItemBlock(ClotheslineBlocks.CLOTHESLINE_ANCHOR).setRegistryName(MODID, "clothesline_anchor"),
                new Item().setUnlocalizedName("clothesline.clotheslineCrank").setRegistryName(MODID, "clothesline_crank"),
                new Item().setUnlocalizedName("clothesline.pulleyWheel").setRegistryName(MODID, "pulley_wheel")
        );
    }

    @SubscribeEvent
    public void attachWorldCapabilities(AttachCapabilitiesEvent<World> event) {
        event.addCapability(new ResourceLocation(MODID, "network_manager"), new NetworkManagerProvider(event.getObject()));
    }

    @SubscribeEvent
    public void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(new ResourceLocation(MODID, "attacher"), new AttacherProvider());
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
            INetworkManager manager = event.getWorld().getCapability(NETWORK_MANAGER_CAPABILITY, null);
            if (manager != null) {
                networkWrapper.sendTo(new MessageSyncNetworks(manager.getNetworks()), player);
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            INetworkManager manager = event.world.getCapability(NETWORK_MANAGER_CAPABILITY, null);
            if (manager != null) {
                manager.update();
            }
        }
    }
}
