package de.cubeisland.cubeengine.auctions.auction;

import de.cubeisland.cubeengine.auctions.AuctionsConfiguration;
import de.cubeisland.cubeengine.auctions.CubeAuctions;
import de.cubeisland.cubeengine.auctions.Sorter;
import de.cubeisland.cubeengine.auctions.auction.timer.AuctionTimer;
import de.cubeisland.cubeengine.core.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Faithcaio
 */
public class AuctionManager
{
    //TODO 2 storages for auctions
    private Map<Integer, Auction> activeAuctions;
    private Map<Integer, Auction> endedAuctions;
    private AuctionsConfiguration config;

    public AuctionManager()
    {
        this.activeAuctions = new HashMap<Integer, Auction>();
        this.endedAuctions = new HashMap<Integer, Auction>();
        this.config = CubeAuctions.getInstance().getConfiguration();
    }

    public void registerAuction(Auction auction)
    {
        this.activeAuctions.put(auction.getKey(), auction);
    }

    public void registerEndingAuction(Auction auction)
    {
        this.endedAuctions.put(auction.getKey(), auction);
    }

    public void startAuction(Bidder owner, ItemStack item, long duration)
    {
        this.startAuction(owner, item, duration, StringUtils.convertTimeToMillis(config.default_length));
    }

    public boolean startAuction(Bidder bidder, ItemStack item, long duration, double startbid)
    {
        Auction auction = new Auction(bidder, item, duration + System.currentTimeMillis());
        //TODO check if auction can be started

        //TODO store auction in db
        this.pushbid(auction, bidder, startbid);
        this.registerAuction(auction);
        AuctionTimer.startTimer();
        CubeAuctions.debug("START auction");
        return true;
    }

    public void pushbid(Auction auction, Bidder bidder, double amount)
    {
        Bid bid = new Bid(bidder, auction, amount);
        auction.pushBid(bid);
        //TODO store bid in db
    }

    public boolean bid(Auction auction, Bidder bidder, double amount)
    {

        if (auction.getTopBid().getAmount() <= amount)//TODO check bid amount
        {
            //t("auc_bid_low2"));
            return false;
        }
        double moneyNeeded = bidder.getTotalBidAmount() + amount;
        if (1 == 0)//TODO check money
        {
            //TODO bypass money permission
            //auc_bid_money1 and 2
            return false;
        }
        this.pushbid(auction, bidder, amount);
        return true;
    }

    public void popBid(Auction auction)
    {
        auction.popBid();
    }

    public boolean undobid(Auction auction, Bidder bidder)
    {
        //TODO msgs
        if (auction.isOwner(bidder))
        {
            //undo_pro2
            return false;
        }
        if (!auction.isTopBidder(bidder))
        {
            // undo_bidder
            return false;
        }
        if (config.undotime > 0)
        {
            if (System.currentTimeMillis() - auction.getTopBid().getTimestamp() > config.undotime * 1000)
            {
                // undo_time
                return false;
            }
        }
        auction.popBid();
        //TODO update DB
        //undo_bid_n
        return true;

    }

    public void endAuction(Auction auction)
    {
        if (auction.isTopBidder(auction.getOwner()))
        {
            double comission = auction.getTopBid().getAmount() * config.comission / 100;
            //TODO charge comission to User
            //TODO t("time_pun4
            this.abortAuction(auction);
            return;
        }
        this.finishAuction(auction, null);
    }

    private void finishAuction(Auction auction, HashSet<Bidder> punished)
    {
        Bid bid = auction.getTopBid();

        if (bid.getBidder().equals(auction.getOwner()))
        {
            //all Bidder had not enough money
            this.abortAuction(auction);
        }
        if (punished.contains(bid.getBidder()))
        {
            //User was already punished
            auction.popBid();
            this.finishAuction(auction, punished);
            return;
        }
        //TODO check money
        if (1 == 0)
        {
            if (punished == null)
            {
                punished = new HashSet<Bidder>();
            }
            double punish = bid.getAmount() * config.punish;
            //TODO punish player
            //TODO msgs...
            //time_pun1 - 3
            auction.popBid();
            punished.add(bid.getBidder());
            //get next Winner
            this.finishAuction(auction, punished);
            return;
        }
        //TODO take money
        Bidder bidder = bid.getBidder();
        // t("time_sold" (msg to owner)
        this.notify(bidder, Bidder.NOTIFY_WIN);
        this.notify(bidder, Bidder.NOTIFY_ITEMS);
        //TODO notify or set notifyState
        //TODO adjust av. Price
        this.activeAuctions.remove(auction.getKey());
        this.registerEndingAuction(auction);
        auction.getTopBid().getBidder().addToBox(auction);
        //TODO move auction to other storage in db
        CubeAuctions.debug("END auction");
    }

    public void notify(Bidder bidder, byte notifyState)
    {
        if (bidder.getUser().isOnline())
        {
            switch (notifyState)
            {
                case Bidder.NOTIFY_CANCEL://TODO msg 
                    return;
                case Bidder.NOTIFY_WIN://TODO msg time_won
                    return;
            }
            return;
        }
        bidder.getNotifyState().set(notifyState);
    }

    public void abortAuction(Auction auction)
    {
        //remove all bids but startbid
        Bid bid = auction.getBids().firstElement();
        auction.getBids().clear();
        auction.pushBid(bid);
        this.activeAuctions.remove(auction.getKey());
        this.registerEndingAuction(auction);
        auction.setEndedTime();
        auction.getOwner().addToBox(auction);
        //TODO move auction to other storage in db
        //TODO retain only last Bid and move to other auction database
        //TODO msg _(auction was not succesful
        this.notify(bid.getBidder(), Bidder.NOTIFY_CANCEL);
        CubeAuctions.debug("ABORT auction");
    }

    public void removeAuction(Auction auction)
    {
        this.activeAuctions.remove(auction.getKey());
        this.endedAuctions.remove(auction.getKey());
        for (Bid bid : auction.getBids())
        {
            //TODO delete bid in db or delete all bids of thus auction...
        }
        auction.setBids(null);
        //TODO delete auction in db

        CubeAuctions.debug("REMOVE auction");
    }

    public List<Auction> getSoonEndingAuctions()
    {
        List<Auction> sortList = new ArrayList(this.activeAuctions.values());
        Sorter.DATE.sortAuction(sortList);
        return sortList;
    }

    public Auction getNextAuktion()
    {
        List<Auction> sortList = this.getSoonEndingAuctions();
        if (sortList.isEmpty())
        {
            return null;
        }
        return sortList.get(0);
    }

    public List<Auction> getAuctionsOf(Bidder bidder)
    {
        List<Auction> list = new ArrayList<Auction>();
        for (Auction auction : this.activeAuctions.values())
        {
            if (auction.isTopBidder(bidder))
            {
                list.add(auction);
            }
        }
        return list;
    }

    public Auction getAuction(Integer id)
    {
        return this.activeAuctions.get(id);
    }
}
