// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.flutter.plugins.googlemobileads;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.OnAdMetadataChangedListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/** A wrapper for {@link RewardedAd}. */
class FlutterRewardedAd extends FlutterAd.FlutterOverlayAd {
  private static final String TAG = "FlutterRewardedAd";

  @NonNull private final AdInstanceManager manager;
  @NonNull private final String adUnitId;
  @NonNull private final FlutterAdLoader flutterAdLoader;
  @Nullable private final FlutterAdRequest request;
  @Nullable private final FlutterAdManagerAdRequest adManagerRequest;
  @Nullable private final FlutterServerSideVerificationOptions serverSideVerificationOptions;
  @Nullable RewardedAd rewardedAd;

  /** A wrapper for {@link RewardItem}. */
  static class FlutterRewardItem {
    @NonNull final Integer amount;
    @NonNull final String type;

    FlutterRewardItem(@NonNull Integer amount, @NonNull String type) {
      this.amount = amount;
      this.type = type;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      } else if (!(other instanceof FlutterRewardItem)) {
        return false;
      }

      final FlutterRewardItem that = (FlutterRewardItem) other;
      if (!amount.equals(that.amount)) {
        return false;
      }
      return type.equals(that.type);
    }

    @Override
    public int hashCode() {
      int result = amount.hashCode();
      result = 31 * result + type.hashCode();
      return result;
    }
  }

  /** Constructor for AdMob Ad Request. */
  public FlutterRewardedAd(
      @NonNull AdInstanceManager manager,
      @NonNull String adUnitId,
      @NonNull FlutterAdRequest request,
      @Nullable FlutterServerSideVerificationOptions serverSideVerificationOptions,
      @NonNull FlutterAdLoader flutterAdLoader) {
    this.manager = manager;
    this.adUnitId = adUnitId;
    this.request = request;
    this.adManagerRequest = null;
    this.serverSideVerificationOptions = serverSideVerificationOptions;
    this.flutterAdLoader = flutterAdLoader;
  }

  /** Constructor for Ad Manager Ad request. */
  public FlutterRewardedAd(
      @NonNull AdInstanceManager manager,
      @NonNull String adUnitId,
      @NonNull FlutterAdManagerAdRequest adManagerRequest,
      @Nullable FlutterServerSideVerificationOptions serverSideVerificationOptions,
      @NonNull FlutterAdLoader flutterAdLoader) {
    this.manager = manager;
    this.adUnitId = adUnitId;
    this.adManagerRequest = adManagerRequest;
    this.request = null;
    this.serverSideVerificationOptions = serverSideVerificationOptions;
    this.flutterAdLoader = flutterAdLoader;
  }

  @Override
  void load() {
    final RewardedAdLoadCallback adLoadCallback =
        new RewardedAdLoadCallback() {
          @Override
          public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
            FlutterRewardedAd.this.rewardedAd = rewardedAd;
            if (serverSideVerificationOptions != null) {
              rewardedAd.setServerSideVerificationOptions(
                  serverSideVerificationOptions.asServerSideVerificationOptions());
            }
            manager.onAdLoaded(FlutterRewardedAd.this, rewardedAd.getResponseInfo());
            super.onAdLoaded(rewardedAd);
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            manager.onAdFailedToLoad(FlutterRewardedAd.this, new FlutterLoadAdError(loadAdError));
          }
        };

    if (request != null) {
      flutterAdLoader.loadRewarded(
          manager.activity, adUnitId, request.asAdRequest(), adLoadCallback);
    } else if (adManagerRequest != null) {
      flutterAdLoader.loadAdManagerRewarded(
          manager.activity, adUnitId, adManagerRequest.asAdManagerAdRequest(), adLoadCallback);
    } else {
      Log.e(TAG, "A null or invalid ad request was provided.");
    }
  }

  @Override
  public void show() {
    if (rewardedAd == null) {
      Log.e(TAG, "The rewarded ad wasn't loaded yet.");
      return;
    }

    rewardedAd.setFullScreenContentCallback(new FlutterFullScreenContentCallback(manager, this));
    rewardedAd.setOnAdMetadataChangedListener(
        new OnAdMetadataChangedListener() {
          @Override
          public void onAdMetadataChanged() {
            manager.onAdMetadataChanged(FlutterRewardedAd.this);
          }
        });
    rewardedAd.show(
        manager.activity,
        new OnUserEarnedRewardListener() {
          @Override
          public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
            manager.onRewardedAdUserEarnedReward(
                FlutterRewardedAd.this,
                new FlutterRewardItem(rewardItem.getAmount(), rewardItem.getType()));
          }
        });
  }
}
