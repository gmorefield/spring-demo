CREATE OR ALTER PROCEDURE SELECT_MANY
    @Count  INT,
    @Msg    VARCHAR(25) = ''
AS
BEGIN
    SET NOCOUNT ON
    SET XACT_ABORT ON;
    DECLARE @itemTable TABLE (id int);

    BEGIN TRY
        BEGIN TRANSACTION

        -- Find the next queued item that is waiting to be processed
        INSERT INTO @itemTable (id)
        SELECT TOP (@Count) id
          FROM workqueue WITH (UPDLOCK, READPAST)
         WHERE status = 'R'
         ORDER BY id

        -- if we've found one, mark it as being processed
        UPDATE workqueue
           SET status = 'I',
               update_dt = getutcdate(),
               msg = (@Msg)
        WHERE id IN (SELECT id FROM @itemTable)
          AND status = 'R';

        COMMIT TRANSACTION

        -- If we've got an item from the queue, return to whatever is going to process it
        SELECT *
          FROM workqueue
         WHERE id IN (SELECT id FROM @itemTable);
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH
END;