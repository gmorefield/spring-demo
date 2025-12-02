CREATE OR ALTER PROCEDURE OUTPUT_MANY_ORDERED_PART
    @Count  INT,
    @Msg    VARCHAR(25) = ''
AS
BEGIN
    SET NOCOUNT ON
    SET XACT_ABORT ON;
    DECLARE @itemTable TABLE (id int);

    BEGIN TRY
        BEGIN TRANSACTION;
--        EXEC sp_getapplock @Resource = 'prefetchManyOrdered', @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 60000;

         UPDATE orderedqueue WITH (UPDLOCK)
           SET status = 'I',
               update_dt = getutcdate(),
               msg = (@Msg)
        OUTPUT inserted.id INTO @itemTable
         WHERE id IN (
                SELECT TOP (@Count) s.id
                  FROM (
                    SELECT row_number() OVER (PARTITION BY order_id ORDER BY id) as rn, id, status
--                        LAG(status) OVER (PARTITION BY order_id ORDER BY id) as prev_status
                      FROM orderedqueue o WITH (UPDLOCK)
                     WHERE o.status != 'C'
                  ) AS s
                 WHERE s.rn = 1
--                   AND s.prev_status = 'C'
                   AND s.status = 'R'
                 ORDER BY s.id
               )
          AND status = 'R';

        COMMIT TRANSACTION;

        -- If we've got an item from the queue, return to whatever is going to process it
        SELECT *
          FROM orderedqueue
         WHERE id IN (SELECT id FROM @itemTable)
         ORDER BY id;

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH

END;