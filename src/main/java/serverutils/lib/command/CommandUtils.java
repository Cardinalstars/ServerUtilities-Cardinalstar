package serverutils.lib.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.DimensionManager;

import serverutils.ServerUtilities;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.math.MathUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.permission.PermissionAPI;

public class CommandUtils {

    public static CommandException error(IChatComponent component) {
        return new CommandException("disconnect.genericReason", component);
    }

    public static ForgePlayer getForgePlayer(ICommandSender sender) throws CommandException {
        ForgePlayer p = Universe.get().getPlayer(sender);

        if (p.isFake()) {
            throw new CommandException("commands.generic.player.notFound", sender.getCommandSenderName());
        }

        return p;
    }

    public static ForgePlayer getForgePlayer(ICommandSender sender, String name) throws CommandException {
        ForgePlayer p;

        switch (name) {
            case "@r": {
                ForgePlayer[] players = Universe.get().getOnlinePlayers().toArray(new ForgePlayer[0]);
                p = players.length == 0 ? null : players[MathUtils.RAND.nextInt(players.length)];
                break;
            }
            case "@ra": {
                ForgePlayer[] players = Universe.get().getPlayers().toArray(new ForgePlayer[0]);
                p = players.length == 0 ? null : players[MathUtils.RAND.nextInt(players.length)];
                break;
            }
            case "@p": {
                if (sender instanceof EntityPlayerMP playerMP && !ServerUtils.isFake(playerMP)) {
                    return Universe.get().getPlayer(sender);
                }

                p = null;
                double dist = Double.POSITIVE_INFINITY;

                for (ForgePlayer p1 : Universe.get().getOnlinePlayers()) {
                    if (p == null) {
                        p = p1;
                    } else {
                        ChunkCoordinates pos = sender.getPlayerCoordinates();
                        double d = p1.getPlayer().getDistanceSq(pos.posX, pos.posY, pos.posZ);

                        if (d < dist) {
                            dist = d;
                            p = p1;
                        }
                    }
                }
                break;
            }
            default: {
                EntityPlayerMP e = PlayerSelector.matchOnePlayer(sender, name);
                if (e == null) {
                    p = Universe.get().getPlayer(name);
                } else {
                    p = Universe.get().getPlayer(e);
                }
            }
        }

        if (p == null || p.isFake()) {
            throw new CommandException("commands.generic.player.notFound", name);
        }

        return p;
    }

    public static ForgeTeam getTeam(ICommandSender sender, String id) throws CommandException {
        ForgeTeam team = Universe.get().getTeam(id);

        if (team.isValid()) {
            return team;
        }

        throw ServerUtilities.error(sender, "serverutilities.lang.team.error.not_found", id);
    }

    public static ForgePlayer getSelfOrOther(ICommandSender sender, String[] args, int index) throws CommandException {
        return getSelfOrOther(sender, args, index, "");
    }

    public static ForgePlayer getSelfOrOther(ICommandSender sender, String[] args, int index,
            String specialPermForOther) throws CommandException {
        if (args.length <= index) {
            return getForgePlayer(sender);
        }

        ForgePlayer p = getForgePlayer(sender, args[index]);

        if (!specialPermForOther.isEmpty() && sender instanceof EntityPlayerMP playerMP
                && !p.getId().equals(playerMP.getUniqueID())
                && !PermissionAPI.hasPermission(playerMP, specialPermForOther)) {
            throw new CommandException("commands.generic.permission");
        }

        return p;
    }

    public static List<String> getDimensionNames() {
        List<String> list = new ArrayList<>();
        list.add("all");
        list.add("overworld");
        list.add("nether");
        list.add("end");

        for (Integer dim : DimensionManager.getStaticDimensionIDs()) {
            if (dim != null && (dim < -1 || dim > 1)) {
                list.add(dim.toString());
            }
        }

        return list;
    }

    public static OptionalInt parseDimension(ICommandSender sender, String[] args, int index) throws CommandException {
        if (args.length <= index) {
            return OptionalInt.empty();
        }

        return switch (args[index].toLowerCase()) {
            case "overworld", "0" -> OptionalInt.of(0);
            case "nether", "-1" -> OptionalInt.of(-1);
            case "end", "1" -> OptionalInt.of(1);
            case "this", "~" -> OptionalInt.of(sender.getEntityWorld().provider.dimensionId);
            case "all", "*" -> OptionalInt.empty();
            default -> OptionalInt.of(CommandBase.parseInt(sender, args[index]));
        };
    }

    public static IChatComponent getTranslatedUsage(ICommand command, ICommandSender sender) {
        String usageS = command.getCommandUsage(sender);
        IChatComponent usage;
        if (usageS == null || usageS.isEmpty()
                || usageS.indexOf('/') != -1
                || usageS.indexOf('%') != -1
                || usageS.indexOf(' ') != -1) {
            usage = new ChatComponentText(usageS);
        } else {
            usage = new ChatComponentTranslation(usageS);
        }
        return usage;
    }

    public static List<ICommand> getAllCommands(ICommandSender sender) {
        ArrayList<ICommand> commands = new ArrayList<>();
        Set<String> cmdIds = new HashSet<>();

        for (ICommand cmd : ServerUtils.getServer().getCommandManager().getPossibleCommands(sender)) {
            if (cmdIds.add(cmd.getCommandName())) {
                commands.add(cmd);
            }
        }

        return commands;
    }
}
