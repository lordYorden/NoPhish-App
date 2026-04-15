import { v } from "convex/values";
import { mutation, query } from "./_generated/server";

export const issue = mutation({
    args: {
        code: v.string(),
        circleId: v.string(),
    },
    handler: async (ctx, args) => {
        const identity = await ctx.auth.getUserIdentity();
        if (!identity) {
            throw new Error("Unauthenticated");
        }

        const expiresAt = Date.now() + 30 * 60 * 1000; // OTP expires in 30 minutes

        await ctx.db.insert("otpCodes", {
            code: args.code,
            expiresAt,
            issuerId: identity.subject,
            circleId: args.circleId,
        });
    }
})

export const redeem = async (
    ctx: any,
    code: string,
    memberId: string,
) => {
    const otpRecord = await ctx.db.query("otpCodes")
        .filter((q: any) => q.eq(q.field("code"), code))
        .first();

    if (!otpRecord) {
        throw new Error("Invalid OTP code");
    }

    if (otpRecord.issuerId === memberId) {
        throw new Error("Cannot redeem OTP code issued by the same user");
    }

    if (otpRecord.memberId) {
        throw new Error("OTP code has already been redeemed");
    }

    if (otpRecord.expiresAt < Date.now()) {
        throw new Error("OTP code has expired");
    }

    await ctx.db.patch(otpRecord._id, {
        memberId,
    });

    return otpRecord.circleId;
}

export const needsotp = query({
    args: {},
    handler: async (ctx) => {
        const identity = await ctx.auth.getUserIdentity();
        if (!identity) {
            throw new Error("Unauthenticated");
        }

        const lastOtp = await ctx.db
            .query("otpCodes")
            .withIndex("byIssuerAndExpiresAt", (q) =>
                q.eq("issuerId", identity.subject),
            )
            .order("desc")
            .first();

        if (!lastOtp || lastOtp.expiresAt < Date.now() || lastOtp.memberId) {
            return "GENERATE"; 
        }
        
        return lastOtp.code;
    }
});

