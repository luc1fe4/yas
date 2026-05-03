import styles from '../styles/404.module.css';

const NotfoundPage = () => {
  return (
    <div className={styles['container']}>
      <h1 className={styles['first-four']}>4</h1>
      <div className={styles['cog-wheel1']}>
        <div className={styles['cog1']}>
          <div className={styles['top']}></div>
          <div className={styles['down']}></div>
          <div className={styles['left-top']}></div>
          <div className={styles['left-down']}></div>
          <div className={styles['right-top']}></div>
          <div className={styles['right-down']}></div>
          <div className={styles['left']}></div>
          <div className={styles['right']}></div>
        </div>
      </div>

      <div className={styles['cog-wheel2']}>
        <div className={styles['cog2']}>
          <div className={styles['top']}></div>
          <div className={styles['down']}></div>
          <div className={styles['left-top']}></div>
          <div className={styles['left-down']}></div>
          <div className={styles['right-top']}></div>
          <div className={styles['right-down']}></div>
          <div className={styles['left']}></div>
          <div className={styles['right']}></div>
        </div>
      </div>
      <h1 className={styles['second-four']}>4</h1>
      <p className={styles['wrong-para']}>Uh Oh! Page not found! hehehehe</p>
    </div>
  );
};

export default NotfoundPage;

// This file is for the 404 page, which will be displayed when a user tries to access a page that does not exist. The page includes two large "4" characters and two cog wheels, along with a message indicating that the page was not found. The styles for the page are defined in the '404.module.css' file.
