// This file is part of OpenTSDB.
// Copyright (C) 2012018  The OpenTSDB Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package net.opentsdb.data;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

import net.opentsdb.common.Const;

/**
 * Simple implementation of a timestamp that is stored as a {@link Long} 
 * in Unix Epoch seconds. Always returns the UTC time zone.
 * <p>
 * <b>Notes on {@link #snapToPreviousInterval(long, ChronoUnit, DayOfWeek)}:</b>
 * <p>
 * @since 3.0
 */
public class SecondTimeStamp implements TimeStamp, Comparable<SecondTimeStamp> {
  /** The timestamp. */
  private long timestamp;
  
  /**
   * Ctor, initializes the timestamp.
   * @param timestamp A timestamp in Unix epoch seconds.
   */
  public SecondTimeStamp(final long timestamp) {
    this.timestamp = timestamp;
  }
  
  @Override
  public long nanos() {
    return 0;
  }
  
  @Override
  public long msEpoch() {
    return timestamp * 1000L;
  }

  @Override
  public long epoch() {
    return timestamp;
  }

  @Override
  public TimeStamp getCopy() {
    return new SecondTimeStamp(timestamp);
  }

  @Override
  public void updateMsEpoch(final long timestamp) {
    this.timestamp = timestamp / 1000;
  }

  @Override
  public void updateEpoch(final long timestamp) {
    this.timestamp = timestamp;
  }
  
  @Override
  public void update(final TimeStamp timestamp) {
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null.");
    }
    if (timestamp instanceof SecondTimeStamp) {
      this.timestamp = ((SecondTimeStamp) timestamp).timestamp;
    } else {
      this.timestamp = timestamp.epoch();
    }
  }

  @Override
  public void update(final long epoch, final long nano) {
    timestamp = epoch;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    if (o == this) {
      return true;
    }
    if (!(o instanceof TimeStamp)) {
      return false;
    }
    return compare(Op.EQ, (TimeStamp) o);
  }
  
  @Override
  public boolean compare(final Op comparator, 
      final TimeStamp compareTo) {
    if (compareTo == null) {
      throw new IllegalArgumentException("Timestamp cannot be null.");
    }
    if (comparator == null) {
      throw new IllegalArgumentException("Comparator cannot be null.");
    }
    switch (comparator) {
    case LT:
      if (epoch() == compareTo.epoch()) {
        if (nanos() < compareTo.nanos()) {
          return true;
        }
      } else if (epoch() < compareTo.epoch()) {
        return true;
      }
      return false;
    case LTE:
      if (epoch() == compareTo.epoch()) {
        if (nanos() <= compareTo.nanos()) {
          return true;
        }
      } else if (epoch() <= compareTo.epoch()) {
        return true;
      }
      return false;
    case GT:
      if (epoch() == compareTo.epoch()) {
        if (nanos() > compareTo.nanos()) {
          return true;
        }
      } else if (epoch() > compareTo.epoch()) {
        return true;
      }
      return false;
    case GTE:
      if (epoch() == compareTo.epoch()) {
        if (nanos() >= compareTo.nanos()) {
          return true;
        }
      } else if (epoch() >= compareTo.epoch()) {
        return true;
      }
      return false;
    case EQ:
      return epoch() == compareTo.epoch() && 
             nanos() == compareTo.nanos();
    case NE:
      return epoch() != compareTo.epoch() || 
             nanos() != compareTo.nanos();
    default:
      throw new UnsupportedOperationException("Unknown comparator: " + comparator);
    }
  }

  @Override
  public void setMax() {
    timestamp = Long.MAX_VALUE;
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("timestamp=")
        .append(timestamp)
        .append(", utc=")
        .append(Instant.ofEpochSecond(timestamp))
        .append(", epoch=")
        .append(epoch())
        .append(", nanos=")
        .append(nanos())
        .append(", msEpoch=")
        .append(msEpoch())
        .toString();
  }

  @Override
  public ChronoUnit units() {
    return ChronoUnit.SECONDS;
  }
  
  @Override
  public ZoneId timezone() {
    return ZoneId.of("UTC");
  }

  @Override
  public void add(final TemporalAmount amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null.");
    }
    if (amount instanceof Duration) {
      timestamp += ((Duration) amount).getSeconds();
    } else {
      // can't shortcut easily here since we don't *know* the number of days in 
      // a month. So snap to a calendar
      final ZonedDateTime zdt = ZonedDateTime.ofInstant(
          Instant.ofEpochSecond(timestamp), Const.UTC);
      timestamp = Instant.from(zdt.plus(amount)).getEpochSecond();
    }
  }
  
  @Override
  public void subtract(final TemporalAmount amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null.");
    }
    if (amount instanceof Duration) {
      timestamp -= ((Duration) amount).getSeconds();
    } else {
      // can't shortcut easily here since we don't *know* the number of days in 
      // a month. So snap to a calendar
      final ZonedDateTime zdt = ZonedDateTime.ofInstant(
          Instant.ofEpochSecond(timestamp), Const.UTC);
      timestamp = Instant.from(zdt.minus(amount)).getEpochSecond();
    }
  }

  @Override
  public void snapToPreviousInterval(final long interval, final ChronoUnit units) {
    snapToPreviousInterval(interval, units, DayOfWeek.SUNDAY);
  }

  @Override
  public void snapToPreviousInterval(final long interval, 
                                     final ChronoUnit units,
                                     final DayOfWeek day_of_week) {
    final TimeStamp snapper = new ZonedNanoTimeStamp(timestamp * 1000, Const.UTC);
    snapper.snapToPreviousInterval(interval, units, day_of_week);
    timestamp = snapper.epoch();
  }

  @Override
  public int compareTo(SecondTimeStamp o) {
    if (timestamp == o.timestamp) {
      return 0;
    }
    return timestamp > o.timestamp ? 1 : -1;
  }

}