-- 09_fresh.sql
-- Wipes all transactional/derived data and rebuilds a realistic 30-day cash history,
-- 6-7 transactions/day/branch, ending TODAY (relative to SYSDATE, so this stays useful
-- no matter when you run it — not tied to a fixed calendar date like the older seed files).
--
-- BR001/BR002 are deposit-biased -> trend toward SURPLUS by day 30.
-- BR003/BR004 are withdrawal-biased -> trend toward/below their threshold -> DEFICIT by day 30.
-- BRANCH.CURRENT_RESERVE is reconciled from the generated ledger at the end, so the "10-day
-- ledger" / "cash-analysis" screens (which walk backward from CURRENT_RESERVE using the
-- transaction history) stay internally consistent — no drift between the two.

DELETE FROM TRANSFER_REQUEST;
DELETE FROM FORECAST_SNAPSHOT;
DELETE FROM CASH_TRANSACTION;

UPDATE BRANCH SET CURRENT_RESERVE = OPENING_RESERVE;

COMMIT;

DECLARE
  TYPE branch_ids_t IS TABLE OF VARCHAR2(10);
  branch_ids branch_ids_t := branch_ids_t('BR001', 'BR002', 'BR003', 'BR004');

  -- % chance a given transaction is a DEPOSIT, per branch. Skewed so two branches trend up
  -- (surplus) and two trend down (deficit) over the 30-day window.
  -- Kept close to 50% deliberately: ~200 transactions accumulate over 30 days, so even a
  -- small skew compounds into a large swing. 58/55 trend gently up, 44/40 trend down toward
  -- (and past) threshold without exploding to multiples of the opening reserve.
  TYPE deposit_pct_t IS TABLE OF NUMBER;
  deposit_pct deposit_pct_t := deposit_pct_t(58, 55, 44, 33);

  running_reserve NUMBER;
  txn_count       PLS_INTEGER;
  is_deposit      BOOLEAN;
  amt             NUMBER;
  txn_ts          TIMESTAMP;
  new_id          VARCHAR2(32);
BEGIN
  FOR b IN 1 .. branch_ids.COUNT LOOP
    SELECT OPENING_RESERVE INTO running_reserve FROM BRANCH WHERE BRANCH_ID = branch_ids(b);

    FOR day_offset IN REVERSE 0 .. 29 LOOP
      txn_count := TRUNC(DBMS_RANDOM.VALUE(6, 8)); -- 6 or 7

      FOR t IN 1 .. txn_count LOOP
        is_deposit := DBMS_RANDOM.VALUE(0, 100) < deposit_pct(b);
        amt := ROUND(DBMS_RANDOM.VALUE(5000, 50000), 2);

        IF is_deposit THEN
          running_reserve := running_reserve + amt;
        ELSE
          -- Never let seed history itself push a branch deeply negative; a real branch
          -- wouldn't be allowed to. Clamp so it can still run low (-> DEFICIT vs its
          -- threshold) without going absurdly negative.
          IF running_reserve - amt < -20000 THEN
            amt := GREATEST(running_reserve + 20000, 0);
          END IF;
          running_reserve := running_reserve - amt;
        END IF;

        IF amt > 0 THEN
          -- Business hours, 09:00-17:00, spread across the day.
          txn_ts := TRUNC(SYSDATE) - day_offset
                    + (9 / 24) + (DBMS_RANDOM.VALUE(0, 8 * 60) / (24 * 60));
          new_id := RAWTOHEX(SYS_GUID());

          INSERT INTO CASH_TRANSACTION (TRANSACTION_ID, BRANCH_ID, TXN_TYPE, AMOUNT, EVENT_TIMESTAMP)
          VALUES (new_id, branch_ids(b), CASE WHEN is_deposit THEN 'DEPOSIT' ELSE 'WITHDRAWAL' END, amt, txn_ts);
        END IF;
      END LOOP;
    END LOOP;
  END LOOP;
END;
/

-- Reconcile CURRENT_RESERVE with the ledger just generated, so branch-service and
-- cash-management-service agree on the same numbers.
UPDATE BRANCH br
SET CURRENT_RESERVE = OPENING_RESERVE + NVL((
  SELECT SUM(CASE WHEN TXN_TYPE = 'DEPOSIT' THEN AMOUNT ELSE -AMOUNT END)
  FROM CASH_TRANSACTION ct WHERE ct.BRANCH_ID = br.BRANCH_ID
), 0);

COMMIT;
