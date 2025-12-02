CREATE OR ALTER PROCEDURE OUTPUT_NEXT
    @Msg VARCHAR(25) = ''
AS
BEGIN
    SET NOCOUNT ON
    DECLARE @NextId INTEGER

    UPDATE workqueue WITH (ROWLOCK)
       SET status = 'I',
           update_dt = getutcdate(),
           msg = (@Msg),
           @NextId = id
     WHERE id IN (
            SELECT TOP 1 w2.id
              FROM workqueue w2 WITH (UPDLOCK, READPAST)
             WHERE w2.status = 'R'
             ORDER BY w2.id
           )
      AND status = 'R';

    -- If we've got an item from the queue, return to whatever is going to process it
    SELECT *
      FROM workqueue
     WHERE id = @NextId;
END;