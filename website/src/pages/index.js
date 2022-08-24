import React from 'react';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useBaseUrl from '@docusaurus/useBaseUrl';
import Layout from '@theme/Layout';
import Head from '@docusaurus/Head';
import CodeBlock from '@theme/CodeBlock';
import clsx from 'clsx';
import styles from './styles.module.css';

const features = [
  {
    title: 'Convenient',
    content: [
      "Working with Temporal is nature like it was developed for Scala."
    ]
  },
  {
    title: 'Robust',
    content: [
      "Most typical errors are handled by the library at compile time.",
      "No more runtime exceptions!"
    ]
  },
  {
    title: 'ZIO-native',
    content: [
      "Use your favorite library with Temporal!",
      "Running ZIO code inside your workflows never been that easy!"
    ]
  },
];

const exampleCode = `import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.protobuf.syntax._

def proceedTransaction(sender: UUID, receiver: UUID, amount: BigDecimal) =
  for {
    transactionId <- ZIO.randomWith(_.nextUUID)
    paymentWorkflow <- client
                         .newWorkflowStub[PaymentWorkflow]
                         .withTaskQueue("payments")
                         .withWorkflowId(transactionId.toString)
                         .build
    _ <- ZWorkflowStub.start(
           paymentWorkflow.proceed(
             ProceedTransactionCommand(
               id = transactionId,
               sender = sender,
               receiver = receiver,
               amount = amount
             )
           )
         )
  } yield transactionId

def confirmTransaction(transactionId: UUID, confirmationCode: String) =
  for {
    paymentWorkflow <- client.newWorkflowStubProxy[PaymentWorkflow](workflowId = transactionId.toString)
    status <- ZWorkflowStub.query(
                paymentWorkflow.getStatus
              )
    _ <- ZIO.when(status.status.isFailed) {
           ZIO.fail(TemporalError(s"Cannot confirm transaction, it's already failed: ${"$"}{status.description}"))
         }
    _ <- ZWorkflowStub.signal(
           paymentWorkflow.confirmTransaction(
             ConfirmTransactionCommand(id = transactionId, confirmationCode)
           )
         )
  } yield ()
`

export default function Home() {
  const context = useDocusaurusContext();
  const { siteConfig = {} } = context;

  return (
    <Layout
      permalink={'/'}
      description={'Build invincible apps with ZIO and Temporal'}
    >
      <Head>
        <meta charset="UTF-8" />
        <meta name="author" content="Vitalii Honta" />
        <meta name="description" content="Build invincible apps with ZIO and Temporal" />
        <meta name="keywords" content="scala, zio, temporal, zio-temporal, workflow management" />
      </Head>
      <div className={clsx('hero hero--dark', styles.heroBanner)}>
        <div className="container">
          {/* <img
             className={clsx(styles.heroBannerLogo, 'margin-vert--md')}
             alt="Create React App logo"
             src={useBaseUrl('img/logo.svg')}
           /> */}
          <h1 className="hero__title">{siteConfig.title}</h1>
          <p className="hero__subtitle">{siteConfig.tagline}</p>

          <div className={styles.getStarted}>
            <Link
              className="button button--outline button--primary button--lg"
              to={useBaseUrl('docs/core/overview')}
            >
              Get Started
            </Link>
          </div>
        </div>
      </div>
      {features && features.length && (
        <div className={styles.features}>
          <div className="container">
            <div className="row">
              {features.map(({ title, content }, idx) => (
                <div key={idx} className={clsx('col col--4', styles.feature)}>
                  <h2>{title}</h2>
                  {content.map(line => (<p>{line}</p>))}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
      <div className={styles.gettingStartedSection}>
        <div className="container padding-vert--xl text--left">
          <div className="row">
            <div className="col col--10 col--offset-1">
              <h2>Get started easily</h2>
              <p>Here is an example of how simple it is to run and interact with Temporal workflows.</p>
              <br />
              <CodeBlock className={clsx("language-scala", styles.exampleCodeBlock)}>
                {exampleCode}
              </CodeBlock>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}