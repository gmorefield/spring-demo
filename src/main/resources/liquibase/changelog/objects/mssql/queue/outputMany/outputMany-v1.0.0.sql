CREATE OR ALTER PROCEDURE OUTPUT_MANY
    @Count  INT,
    @Msg    VARCHAR(25) = ''
AS
BEGIN
    SET NOCOUNT ON
    DECLARE @itemTable TABLE (id int);

    UPDATE workqueue WITH (ROWLOCK)
       SET status = 'I',
           update_dt = getutcdate(),
           msg = (@Msg)
    OUTPUT inserted.id INTO @itemTable
     WHERE id IN (
            SELECT TOP (@Count) w2.id
              FROM workqueue w2 WITH (UPDLOCK, READPAST)
             WHERE w2.status = 'R'
             ORDER BY w2.id
           )
      AND status = 'R';

    -- If we've got an item from the queue, return to whatever is going to process it
    SELECT *
      FROM workqueue
     WHERE id IN (SELECT id FROM @itemTable);
END;