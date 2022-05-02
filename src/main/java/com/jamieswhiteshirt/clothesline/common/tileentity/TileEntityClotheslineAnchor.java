package com.jamieswhiteshirt.clothesline.common.tileentity;

import com.jamieswhiteshirt.clothesline.Clothesline;
import com.jamieswhiteshirt.clothesline.api.INetwork;
import com.jamieswhiteshirt.clothesline.api.INetworkManager;
import com.jamieswhiteshirt.clothesline.api.INetworkNode;
import com.jamieswhiteshirt.clothesline.api.INetworkState;
import com.jamieswhiteshirt.clothesline.common.Util;
import com.jamieswhiteshirt.clothesline.common.impl.NetworkItemHandler;
import com.jamieswhiteshirt.clothesline.common.network.message.SetAnchorHasCrankMessage;
import mysticalmechanics.api.DefaultMechCapability;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

import static mysticalmechanics.api.MysticalMechanicsAPI.MECH_CAPABILITY;

public class TileEntityClotheslineAnchor extends TileEntity implements ITickable {
    @CapabilityInject(IItemHandler.class)
    private static final Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = Util.nonNullInjected();
    private INetworkManager manager;
    private boolean hasCrank;
    private boolean canInsert = false;
    private boolean initflag = false;
    private IItemHandler neighbourHandler;
    private int consPower = 0;
    EnumFacing connect;

    public boolean getHasCrank() {
        return hasCrank;
    }

    public void setHasCrank(boolean hasCrank) {
        this.hasCrank = hasCrank;
        if (!world.isRemote) {
            Clothesline.instance.networkChannel.sendToAllTracking(
                new SetAnchorHasCrankMessage(pos, hasCrank),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 0)
            );

        }
    }


    @Nullable
    public INetworkNode getNetworkNode() {
        if (manager != null) {
            return manager.getNetworks().getNodes().get(pos);
        } else {
            return null;
        }
    }

    public void crank(int amount) {
        INetworkNode node = getNetworkNode();
        if (node != null) {
            INetworkState networkState = node.getNetwork().getState();
            networkState.setMomentum(networkState.getMomentum() + amount);
        }
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        manager = world.getCapability(Clothesline.NETWORK_MANAGER_CAPABILITY, null);


    }

    @Override
    public void update() {
        INetworkNode node = getNetworkNode();
        if (node != null && consPower != 0) {
            INetworkState networkState = node.getNetwork().getState();
            if(networkState.getMomentum() < consPower)
                networkState.setMomentum(networkState.getMomentum() + consPower / 3);
        }


        if(!initflag) {
            connect = EnumFacing.byIndex(this.getBlockMetadata()).getOpposite();
            TileEntity toInsert = world.getTileEntity(getPos().add(connect.getXOffset(), connect.getYOffset(), connect.getZOffset()));
            if(toInsert != null && toInsert.hasCapability(ITEM_HANDLER_CAPABILITY, connect)){
                canInsert = true;
                neighbourHandler = toInsert.getCapability(ITEM_HANDLER_CAPABILITY, connect);
            }
            initflag = true;
        }

        if(canInsert && world != null && !world.isRemote) {
            IItemHandler netHandler = getCapability(ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
            if(netHandler == null) return;
            ItemStack stack = netHandler.extractItem(0, 1, true);
            if(!stack.isEmpty()) {
                int i;
                for (i = 0; i < neighbourHandler.getSlots(); i++) {
                    stack = neighbourHandler.insertItem(i, stack, false);
                    if(stack.getCount() == 0){
                        netHandler.extractItem(0, 1, false);
                        break;
                    }
                }


            }
        }
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == ITEM_HANDLER_CAPABILITY && facing != null) {
            INetworkNode node = getNetworkNode();
            if (node != null && !node.getNetwork().getState().getPath().isEmpty()) {
                INetwork network = node.getNetwork();
                int attachmentKey = network.getState().offsetToAttachmentKey(node.getPathNode().getOffsetForDelta(facing.getDirectionVec()));
                return ITEM_HANDLER_CAPABILITY.cast(new NetworkItemHandler(network, attachmentKey));
            }
        }
        if (capability == MECH_CAPABILITY && (facing == EnumFacing.DOWN || facing == EnumFacing.UP)) {
            return (T) new AnchorMechCapability();
        }
        return null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == ITEM_HANDLER_CAPABILITY && facing != null) {
            INetworkNode node = getNetworkNode();
            return node != null && !node.getNetwork().getState().getPath().isEmpty();
        }
        if (capability == MECH_CAPABILITY && (facing == EnumFacing.DOWN || facing == EnumFacing.UP)) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("HasCrank", hasCrank);
        compound.setInteger("ConstantPower", consPower);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        hasCrank = compound.getBoolean("HasCrank");
        consPower = compound.getInteger("ConstantPower");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    public class AnchorMechCapability extends DefaultMechCapability implements INBTSerializable<NBTTagCompound> {



        @Override
        public void setPower(double value, EnumFacing from) {
            super.setPower(value, from);
            if(from == EnumFacing.DOWN || from == EnumFacing.UP) {
                consPower = (int) value;
                power = value;
            }
        }

        @Override
        public double getVisualPower(EnumFacing from) {
            return super.getVisualPower(from);
        }

        @Override
        public boolean isInput(EnumFacing from) {
            return true;
        }

        @Override
        public boolean isOutput(EnumFacing from) {
            return false;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setDouble("power", power);
            return nbt;
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            power = nbt.getDouble("power");
        }
    }
}
